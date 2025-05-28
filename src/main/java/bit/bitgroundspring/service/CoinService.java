package bit.bitgroundspring.service;

import bit.bitgroundspring.entity.Coin;
import bit.bitgroundspring.repository.CoinRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // @Transactional 임포트 유지
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional // 트랜잭션 관리 활성화
public class CoinService {

    private final CoinRepository coinRepository;
    private final WebClient upbitWebClient;
    private final ObjectMapper objectMapper;

    public CoinService(CoinRepository coinRepository, WebClient.Builder webClientBuilder) {
        this.coinRepository = coinRepository;
        this.upbitWebClient = webClientBuilder.baseUrl("https://api.upbit.com").build();
        this.objectMapper = new ObjectMapper();
    }

    public void collectAndSaveAllKrwCoins() {
        // System.out.println("Starting Upbit data collection for all KRW markets..."); // 시작 로그는 선택 사항, 스케줄러에서 이미 출력
        try {
            List<Map<String, Object>> allMarkets = upbitWebClient.get()
                    .uri("/v1/market/all?isDetails=true")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();

            List<String> krwMarketSymbols = allMarkets.stream()
                    .filter(m -> m.get("market") instanceof String && ((String) m.get("market")).startsWith("KRW-"))
                    .map(m -> (String) m.get("market"))
                    .collect(Collectors.toList());

            Map<String, Map<String, Object>> krwMarketDetailsMap = allMarkets.stream()
                    .filter(m -> krwMarketSymbols.contains((String) m.get("market")))
                    .collect(Collectors.toMap(m -> (String) m.get("market"), m -> m));

            final int BATCH_SIZE = 100;
            for (int i = 0; i < krwMarketSymbols.size(); i += BATCH_SIZE) {
                List<String> batchSymbols = krwMarketSymbols.subList(i, Math.min(i + BATCH_SIZE, krwMarketSymbols.size()));
                String marketsParam = String.join(",", batchSymbols);

                List<Map<String, Object>> tickers = upbitWebClient.get()
                        .uri("/v1/ticker?markets={markets}", marketsParam)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                        .block();

                for (Map<String, Object> ticker : tickers) {
                    String symbol = (String) ticker.get("market");
                    Optional<Coin> existingCoin = coinRepository.findBySymbol(symbol);
                    Coin coin = existingCoin.orElseGet(() -> Coin.builder().build());

                    coin.setSymbol(symbol);

                    if (ticker.get("signed_change_rate") != null) {
                        coin.setChangeRate(Float.parseFloat(ticker.get("signed_change_rate").toString()) * 100);
                    }

                    // --- 디버그 로깅 코드 제거됨 ---
                    if (ticker.get("acc_trade_price_24h") != null) {
                        Object rawTradePrice = ticker.get("acc_trade_price_24h");
                        try {
                            if (rawTradePrice instanceof Number) {
                                coin.setTradePrice24h(((Number) rawTradePrice).longValue());
                            } else {
                                double doubleValue = Double.parseDouble(rawTradePrice.toString());
                                coin.setTradePrice24h((long) doubleValue);
                            }
                        } catch (NumberFormatException | ClassCastException e) {
                            // 오류 로깅은 Logback/SLF4J를 사용하는 것이 좋습니다.
                            // System.err.println("Error parsing acc_trade_price_24h for " + symbol + ": " + rawTradePrice + " -> " + e.getMessage());
                            coin.setTradePrice24h(0L);
                        }
                    } else {
                        coin.setTradePrice24h(0L);
                    }
                    // --- 디버그 로깅 코드 제거됨 끝 ---


                    Map<String, Object> details = krwMarketDetailsMap.get(symbol);
                    if (details != null) {
                        coin.setKoreanName((String) details.getOrDefault("korean_name", symbol));
                        Map<String, Object> marketEvent = (Map<String, Object>) details.get("market_event");
                        if (marketEvent != null) {
                            coin.setIsWarning((Boolean) marketEvent.getOrDefault("warning", false));

                            Map<String, Object> caution = (Map<String, Object>) marketEvent.get("caution");
                            boolean hasAnyCautionFlag = false;
                            if (caution != null) {
                                for (Object value : caution.values()) {
                                    if (value instanceof Boolean && (Boolean) value) {
                                        hasAnyCautionFlag = true;
                                        break;
                                    }
                                }
                            }
                            coin.setIsCaution(hasAnyCautionFlag);
                        }
                    }
                    coinRepository.save(coin);
                }
            }

            // System.out.println("Upbit data collection for all KRW markets completed successfully. Total coins collected: " + krwMarketSymbols.size()); // 완료 로그도 선택 사항

        } catch (Exception e) {
            // 중요한 오류이므로, System.err.println 대신 SLF4J/Logback을 사용하여 로깅하는 것이 좋습니다.
            // e.printStackTrace();
        }
    }
}