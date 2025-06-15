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
    private volatile boolean isShuttingDown = false;
    
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady() {
        connectToUpbit();
    }
    
    @PreDestroy
    public void shutdown() {
        isShuttingDown = true;
        if (connectionManager != null) {
            try {
                connectionManager.stop();
                session.close();
                Thread.sleep(1000); // 잠시 대기하여 연결 종료
                log.info("WebSocket connection manager stopped");
            } catch (Exception e) {
                log.error("Error stopping connection manager", e);
            }
        }
    }
    
    @Scheduled(fixedDelay = 30000) // 30초마다 연결 상태 체크
    public void checkConnectionHealth() {
        if (!isShuttingDown && !isConnected()) {
            log.warn("WebSocket connection is not healthy, attempting reconnection");
            scheduleReconnect();
        }
    }
    
    @Scheduled(cron = "0 5 0 * * ?") // 매일 0시 5분
    public void updateSymbolList() {
        List<String> activeSymbols = coinService.getActiveSymbols();
        sendSymbolRequest(activeSymbols);
    }
    
    private synchronized void connectToUpbit() {
        if (isShuttingDown) {
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
            scheduleReconnect();
        }
    }
    
    public synchronized void sendSymbolRequest(List<String> symbols) {
        if (!isConnected()) {
            log.warn("Cannot send symbol request - WebSocket session is not available");
            scheduleReconnect();
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
            scheduleReconnect();
        }
    }
    
    private class UpbitWebSocketHandler extends TextWebSocketHandler {
        
        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            UpbitWebSocketService.this.session = session;
            log.info("WebSocket connection established to Upbit");
            
            CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS)
                    .execute(() -> {
                        List<String> activeSymbols = coinService.getActiveSymbols();
                        sendSymbolRequest(activeSymbols);
                    });
        }
        
        @Override
        protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
            try {
                ByteBuffer buffer = message.getPayload();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                String jsonString = new String(bytes, StandardCharsets.UTF_8);
                
                processTickerMessage(jsonString);
                
            } catch (Exception e) {
                log.error("Failed to process binary message", e);
            }
        }
        
        private void processTickerMessage(String jsonString) {
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
                
                redisTemplate.opsForValue().set("price:" + symbol, String.valueOf(price),
                        Duration.ofMinutes(5));
                
                priceUpdateService.checkAndExecuteOrdersAsync(symbol, price);
                
            } catch (Exception e) {
                log.error("Failed to process ticker message: {}", jsonString, e);
            }
        }
        
        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.error("WebSocket transport error", exception);
            UpbitWebSocketService.this.session = null;
            scheduleReconnect();
        }
        
        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            log.warn("WebSocket connection closed: code={}, reason={}",
                    status.getCode(), status.getReason());
            UpbitWebSocketService.this.session = null;
            
            if (!status.equals(CloseStatus.NORMAL) && !isShuttingDown) {
                scheduleReconnect();
            }
        }
    }
    
    private void scheduleReconnect() {
        if (isShuttingDown) {
            return;
        }
        
        synchronized (this) {
            if (isReconnecting) {
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
                    if (!isShuttingDown) {
                        connectToUpbit();
                    }
                });
    }
    
    public boolean isConnected() {
        return session != null && session.isOpen() &&
                connectionManager != null && connectionManager.isRunning();
    }
}