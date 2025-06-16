package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.response.Message;
import bit.bitgroundspring.dto.response.MessageType;
import bit.bitgroundspring.dto.response.NotificationResponse;
import bit.bitgroundspring.entity.Coin;
import bit.bitgroundspring.entity.Order;
import bit.bitgroundspring.entity.OrderType;
import bit.bitgroundspring.entity.Status;
import bit.bitgroundspring.repository.CoinRepository;
import bit.bitgroundspring.repository.OrderRepository;
import bit.bitgroundspring.util.UserSseEmitters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderExecutionService {
    
    private final OrderRepository orderRepository;
    private final CoinRepository coinRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserSseEmitters userSseEmitters;
    
    @Value("${upbit.order.execution.queue}")
    private String executionQueueName;
    
    public void queueOrderExecution(Integer orderId, double executionPrice) {
        try {
            String executionMessage = orderId + ":" + executionPrice + ":" + System.currentTimeMillis();
            redisTemplate.opsForList().leftPush(executionQueueName, executionMessage);
        } catch (Exception e) {
            log.error("Failed to queue order execution: {}", orderId, e);
        }
    }
    
    @Scheduled(fixedDelay = 100)
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
    
    @Transactional
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
            
            // SSE 알림 전송
            int userId = order.getUser().getId();
            OrderType orderType = order.getOrderType();
            String symbol = coinRepository.findById(order.getCoin().getId())
                    .map(Coin::getSymbol)
                    .orElse("Unknown");
            double amount = order.getAmount();
            float tradePrice = order.getTradePrice().intValue();
            Map<String, Object> data = Map.of(
                    "orderType", orderType.name(),
                    "symbol", symbol,
                    "amount", amount,
                    "tradePrice", tradePrice
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
