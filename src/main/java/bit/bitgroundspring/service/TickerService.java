package bit.bitgroundspring.service;
import bit.bitgroundspring.dto.TickerMsg;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class TickerService {
    private final Map<String, Float> latest = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        connect();
    }

    private void connect() {
        StandardWebSocketClient client = new StandardWebSocketClient();
        TextWebSocketHandler handler = new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                // 초기 구독 페이로드: 원하는 마켓을 배열로 지정
                String subscribePayload = "[{\"ticket\":\"T1\"},{\"type\":\"trade\",\"codes\":[\"KRW-BTC\",\"KRW-ETH\"]}]";
                session.sendMessage(new TextMessage(subscribePayload));
            }

            @Override
            public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                TickerMsg msg = parseTicker(message.getPayload());
                latest.put(msg.getMarket(), msg.getTradePrice());
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                // 연결이 끊기면 5초 후 재연결
                Executors.newSingleThreadScheduledExecutor()
                        .schedule(TickerService.this::connect, 5, TimeUnit.SECONDS);
            }
        };
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        // deprecated doHandshake 대신 execute 사용
        client.execute(handler, headers, URI.create("wss://api.upbit.com/websocket/v1"));
    }

    /**
     * 주어진 심볼의 최신 거래 가격을 반환합니다.
     */
    public float getLatestPrice(String symbol) {
        return latest.getOrDefault(symbol, 0f);
    }

    /**
     * JSON 페이로드를 TickerMsg 배열로 파싱한 후 첫 번째 요소를 반환합니다.
     */
    private TickerMsg parseTicker(String payload) throws Exception {
        TickerMsg[] arr = objectMapper.readValue(payload, TickerMsg[].class);
        return arr[0];
    }
}