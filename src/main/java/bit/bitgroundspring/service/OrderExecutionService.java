package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.response.Message;
import bit.bitgroundspring.dto.response.MessageType;
import bit.bitgroundspring.dto.response.NotificationResponse;
import bit.bitgroundspring.entity.*;
import bit.bitgroundspring.repository.CoinRepository;
import bit.bitgroundspring.repository.OrderRepository;
import bit.bitgroundspring.repository.UserAssetRepository;
import bit.bitgroundspring.repository.UserRepository;
import bit.bitgroundspring.util.UserSseEmitters;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderExecutionService {
    
    private final OrderRepository orderRepository;
    private final CoinRepository coinRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserSseEmitters userSseEmitters;
    private final UserRepository userRepository;
    private final UserAssetRepository userAssetRepository;
    
    @Value("${upbit.order.execution.queue}")
    private String executionQueueName;
    
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    
    @PreDestroy
    public void shutdown() {
        isShuttingDown.set(true);
        log.info("OrderExecutionService shutting down...");
    }
    
    public void queueOrderExecution(Integer orderId, double executionPrice) {
        // 종료 중이면 큐에 추가하지 않음
        if (isShuttingDown.get()) {
            return;
        }
        
        try {
            String executionMessage = orderId + ":" + executionPrice + ":" + System.currentTimeMillis();
            redisTemplate.opsForList().leftPush(executionQueueName, executionMessage);
        } catch (Exception e) {
            log.error("Failed to queue order execution: {}", orderId, e);
        }
    }
    
    @Scheduled(fixedDelay = 100)
    @Transactional
    public void processOrderExecutionQueue() {
        try {
            Object messageObj = redisTemplate.opsForList().rightPop(executionQueueName);
            
            if (messageObj != null) {
                String message = messageObj.toString();
                String[] parts = message.split(":");
                if (parts.length >= 3) {
                    Integer orderId = Integer.parseInt(parts[0]);
                    double executionPrice = Double.parseDouble(parts[1]);
                    long timestamp = Long.parseLong(parts[2]);
                    
                    if (System.currentTimeMillis() - timestamp > 300000) {
                        log.warn("Discarding old execution message for order: {}", orderId);
                        return;
                    }
                    
                    executeOrder(orderId, executionPrice);
                }
            }
        } catch (Exception e) {
            log.error("Error processing order execution queue", e);
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void executeOrder(Integer orderId, double executionPrice) {
        try {
            Optional<Order> orderOpt = orderRepository.findByIdAndStatus(orderId, Status.PENDING);
            if (orderOpt.isEmpty()) {
                log.debug("Order not found or not pending: {}", orderId);
                return;
            }
            
            Order order = orderOpt.get();
            order.setStatus(Status.COMPLETED);
            order.setTradePrice(executionPrice);
            order.setUpdatedAt(LocalDateTime.now());
            
            orderRepository.save(order);
            
            int userId = order.getUser().getId();
            OrderType orderType = order.getOrderType();
            String symbol = coinRepository.findById(order.getCoin().getId())
                    .map(Coin::getSymbol)
                    .orElse("Unknown");
            double amount = order.getAmount();
            
            // 유저 현재 잔액 업데이트
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (orderType == OrderType.BUY) {
                user.setCash(user.getCash() - (int) (executionPrice * amount));
            } else if (orderType == OrderType.SELL) {
                user.setCash(user.getCash() + (int) (executionPrice * amount));
            }
            userRepository.save(user);
            
            // 유저 자산 업데이트
            Optional<UserAsset> userAssetOptional = userAssetRepository.findByUserAndCoinWithLock(user, order.getCoin());
            if (userAssetOptional.isPresent()) {
                UserAsset userAsset = userAssetOptional.get();
                double userAssetAmount = userAsset.getAmount();
                if (orderType == OrderType.BUY) {
                    userAsset.setAmount(userAssetAmount + amount);
                    userAsset.setAvgPrice(
                            (userAsset.getAvgPrice() * userAssetAmount + executionPrice * amount)
                                    / (userAssetAmount + amount));
                    userAssetRepository.save(userAsset);
                } else if (orderType == OrderType.SELL) {
                    final float EPS = 0.00000001f;
                    double remainingAmount = userAssetAmount - amount;
                    if (remainingAmount < EPS) {
                        userAssetRepository.delete(userAsset);
                    } else {
                        userAsset.setAmount(remainingAmount);
                        userAssetRepository.save(userAsset);
                    }
                }
            } else if (orderType == OrderType.BUY) {
                UserAsset newUserAsset = UserAsset.builder()
                        .user(user)
                        .coin(order.getCoin())
                        .amount(amount)
                        .avgPrice(executionPrice)
                        .build();
                userAssetRepository.save(newUserAsset);
            }
            
            // SSE 알림 전송
            float tradePrice = order.getTradePrice().intValue();
            Map<String, Object> data = Map.of(
                    "orderType", orderType.name(),
                    "symbol", symbol,
                    "amount", amount,
                    "tradePrice", tradePrice,
                    "cash", user.getCash()
            );
            NotificationResponse notificationResponse = NotificationResponse.builder()
                    .messageType(MessageType.INFO)
                    .message(Message.ORDER_EXECUTION)
                    .data(data)
                    .build();
            userSseEmitters.sendToUser(userId, notificationResponse);
            
            log.info("Order executed: {} at price {} for user {}",
                    orderId, executionPrice, order.getUser().getId());
            
        } catch (Exception e) {
            log.error("Failed to execute order: {}", orderId, e);
        }
    }
}
