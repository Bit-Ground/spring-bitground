package bit.bitgroundspring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpbitWebSocketService {
    
    private final CoinService coinService;
    private final PriceUpdateService priceUpdateService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${upbit.websocket.ticker-url}")
    private String websocketUrl;
    
    @Value("${upbit.websocket.connection-timeout}")
    private long connectionTimeout;
    
    @Value("${upbit.websocket.reconnect.initial-delay}")
    private long initialReconnectDelay;
    
    @Value("${upbit.websocket.reconnect.max-delay}")
    private long maxReconnectDelay;
    
    private volatile WebSocketSession session;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebSocketConnectionManager connectionManager;
    
    // 재연결 관련 설정
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private volatile boolean isReconnecting = false;
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady() {
        connectToUpbit();
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down WebSocket connection...");
        
        // 종료 플래그 설정 (가장 먼저 해야 함)
        isShuttingDown.set(true);
        
        try {
            // WebSocket 연결 먼저 종료
            if (session != null && session.isOpen()) {
                session.close(CloseStatus.GOING_AWAY);
                log.info("WebSocket session closed");
            }
        } catch (Exception e) {
            log.warn("Error closing WebSocket session", e);
        }
        
        try {
            // ConnectionManager 종료
            if (connectionManager != null && connectionManager.isRunning()) {
                connectionManager.stop();
                log.info("WebSocket connection manager stopped");
            }
        } catch (Exception e) {
            log.warn("Error stopping connection manager", e);
        }
        
        // 정리 대기
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        session = null;
    }
    
    @Scheduled(fixedDelay = 30000) // 30초마다 연결 상태 체크
    public void checkConnectionHealth() {
        if (!isShuttingDown.get() && !isConnected()) {
            log.warn("WebSocket connection is not healthy, attempting reconnection");
            scheduleReconnect();
        }
    }
    
    @Scheduled(cron = "0 3 0 * * ?") // 매일 0시 3분
    public void updateSymbolList() {
        if (isShuttingDown.get()) {
            return;
        }
        List<String> activeSymbols = coinService.getActiveSymbols();
        sendSymbolRequest(activeSymbols);
    }
    
    private synchronized void connectToUpbit() {
        if (isShuttingDown.get()) {
            return;
        }
        
        try {
            // 기존 연결 정리
            if (connectionManager != null) {
                try {
                    connectionManager.stop();
                } catch (Exception e) {
                    log.warn("Error stopping previous connection manager", e);
                }
            }
            
            StandardWebSocketClient client = new StandardWebSocketClient();
            client.setTaskExecutor(new SimpleAsyncTaskExecutor("WebSocketClient-"));
            client.getUserProperties().put("org.apache.tomcat.websocket.IO_TIMEOUT_MS", String.valueOf(connectionTimeout));
            
            UpbitWebSocketHandler handler = new UpbitWebSocketHandler();
            connectionManager = new WebSocketConnectionManager(client, handler, websocketUrl);
            connectionManager.setAutoStartup(false);
            connectionManager.start();
            
            reconnectAttempts.set(0);
            isReconnecting = false;
            
            log.info("WebSocketConnectionManager started for Upbit WebSocket");
            
        } catch (Exception e) {
            log.error("Failed to connect to Upbit WebSocket (attempt {})",
                    reconnectAttempts.get() + 1, e);
            if (!isShuttingDown.get()) {
                scheduleReconnect();
            }
        }
    }
    
    public synchronized void sendSymbolRequest(List<String> symbols) {
        if (isShuttingDown.get() || !isConnected()) {
            log.warn("Cannot send symbol request - service is shutting down or WebSocket session is not available");
            return;
        }
        
        try {
            List<Object> request = Arrays.asList(
                    Map.of("ticket", "bitground"),
                    Map.of("type", "ticker", "codes", symbols),
                    Map.of("format", "SIMPLE")
            );
            
            String jsonRequest = objectMapper.writeValueAsString(request);
            session.sendMessage(new TextMessage(jsonRequest));
            log.info("Sent symbol request for {} symbols", symbols.size());
            
        } catch (Exception e) {
            log.error("Failed to send symbol request", e);
            if (!isShuttingDown.get()) {
                scheduleReconnect();
            }
        }
    }
    
    private class UpbitWebSocketHandler extends TextWebSocketHandler {
        
        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            if (isShuttingDown.get()) {
                try {
                    session.close();
                } catch (Exception e) {
                    log.warn("Error closing session during shutdown", e);
                }
                return;
            }
            
            UpbitWebSocketService.this.session = session;
            log.info("WebSocket connection established to Upbit");
            
            CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS)
                    .execute(() -> {
                        if (!isShuttingDown.get()) {
                            List<String> activeSymbols = coinService.getActiveSymbols();
                            sendSymbolRequest(activeSymbols);
                        }
                    });
        }
        
        @Override
        protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
            // 종료 중이면 메시지 처리하지 않음
            if (isShuttingDown.get()) {
                return;
            }
            
            try {
                ByteBuffer buffer = message.getPayload();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                String jsonString = new String(bytes, StandardCharsets.UTF_8);
                
                processTickerMessage(jsonString);
                
            } catch (Exception e) {
                if (!isShuttingDown.get()) {
                    log.error("Failed to process binary message", e);
                }
            }
        }
        
        private void processTickerMessage(String jsonString) {
            // 종료 중이면 처리하지 않음
            if (isShuttingDown.get()) {
                return;
            }
            
            try {
                JsonNode tickerData = objectMapper.readTree(jsonString);
                
                if (!tickerData.has("cd") || !tickerData.has("tp")) {
                    return;
                }
                
                String symbol = tickerData.get("cd").asText();
                JsonNode tpNode = tickerData.get("tp");
                
                if (symbol == null || symbol.trim().isEmpty() || tpNode.isNull()) {
                    return;
                }
                
                double price = tpNode.asDouble();
                if (price <= 0) {
                    return;
                }
                
                // Redis 연결 상태 확인 후 저장 (종료 중이 아닐 때만)
                if (!isShuttingDown.get()) {
                    try {
                        redisTemplate.opsForValue().set("price:" + symbol, String.valueOf(price),
                                Duration.ofMinutes(5));
                    } catch (Exception e) {
                        // Redis 연결이 끊어진 경우 로그만 남기고 계속 진행
                        if (!isShuttingDown.get()) {
                            log.debug("Failed to save price to Redis for symbol {}: {}", symbol, e.getMessage());
                        }
                    }
                }
                
                // 주문 실행 체크 (종료 중이 아닐 때만)
                if (!isShuttingDown.get()) {
                    try {
                        priceUpdateService.checkAndExecuteOrdersAsync(symbol, price);
                    } catch (Exception e) {
                        if (!isShuttingDown.get()) {
                            log.error("Failed to check and execute orders for symbol {}", symbol, e);
                        }
                    }
                }
                
            } catch (Exception e) {
                if (!isShuttingDown.get()) {
                    log.error("Failed to process ticker message: {}", jsonString, e);
                }
            }
        }
        
        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            if (!isShuttingDown.get()) {
                log.error("WebSocket transport error", exception);
            }
            UpbitWebSocketService.this.session = null;
            
            if (!isShuttingDown.get()) {
                scheduleReconnect();
            }
        }
        
        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            if (!isShuttingDown.get()) {
                log.warn("WebSocket connection closed: code={}, reason={}",
                        status.getCode(), status.getReason());
            }
            UpbitWebSocketService.this.session = null;
            
            if (!status.equals(CloseStatus.NORMAL) && !isShuttingDown.get()) {
                scheduleReconnect();
            }
        }
    }
    
    private void scheduleReconnect() {
        if (isShuttingDown.get()) {
            return;
        }
        
        synchronized (this) {
            if (isReconnecting || isShuttingDown.get()) {
                return;
            }
            isReconnecting = true;
        }
        
        int currentAttempt = reconnectAttempts.getAndIncrement();
        
        long delay = Math.min(
                initialReconnectDelay * (1L << Math.min(currentAttempt, 6)),
                maxReconnectDelay
        );
        
        log.info("Scheduling reconnection attempt {} in {} ms", currentAttempt + 1, delay);
        
        CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS)
                .execute(() -> {
                    if (!isShuttingDown.get()) {
                        connectToUpbit();
                    }
                });
    }
    
    public boolean isConnected() {
        return !isShuttingDown.get() && session != null && session.isOpen() &&
                connectionManager != null && connectionManager.isRunning();
    }
}