package bit.bitgroundspring.service;

import bit.bitgroundspring.entity.OrderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class PriceUpdateService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderExecutionService orderExecutionService;
    
    @Async("priceUpdateTaskExecutor")
    public CompletableFuture<Void> checkAndExecuteOrdersAsync(String symbol, double currentPrice) {
        // 락(Lock) 관련 로직 제거
        try {
            // 락 획득 과정 없이 바로 주문 처리 로직 호출
            processOrdersForType(symbol, currentPrice, OrderType.BUY);
            processOrdersForType(symbol, currentPrice, OrderType.SELL);
            
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Error processing orders for symbol: {}", symbol, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    private void processOrdersForType(String symbol, double currentPrice, OrderType orderType) {
        String orderTypeKey = (orderType == OrderType.BUY ? "buy_orders:" : "sell_orders:") + symbol;
        Set<Object> orderIds;
        
        if (orderType == OrderType.SELL) {
            orderIds = redisTemplate.opsForZSet().rangeByScore(orderTypeKey, 0, currentPrice);
        } else {
            orderIds = redisTemplate.opsForZSet().rangeByScore(orderTypeKey, currentPrice, Double.MAX_VALUE);
        }
        
        if (orderIds == null || orderIds.isEmpty()) {
            return;
        }
        
        for (Object orderIdObj : orderIds) {
            try {
                String orderId = orderIdObj.toString();
                
                // 주문 정보가 존재하는지 확인 (객체로 저장된 경우)
                Object orderObj = redisTemplate.opsForValue().get("order:" + orderId);
                if (orderObj != null) {
                    orderExecutionService.queueOrderExecution(Integer.parseInt(orderId), currentPrice);
                    
                    // Redis에서 주문 제거
                    redisTemplate.execute(new SessionCallback<Object>() {
                        @Override
                        public Object execute(RedisOperations operations) throws DataAccessException {
                            operations.multi();
                            operations.opsForZSet().remove(orderTypeKey, orderId);
                            operations.delete("order:" + orderId);
                            return operations.exec();
                        }
                    });
                }
            } catch (NumberFormatException e) {
                log.error("Invalid order ID format: {}", orderIdObj, e);
            } catch (Exception e) {
                log.error("Error processing order {}", orderIdObj, e);
            }
        }
    }
}