package bit.bitgroundspring.service;

import bit.bitgroundspring.entity.Coin;
import bit.bitgroundspring.repository.CoinRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent; // 정확한 임포트 경로
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CoinService {

    private final CoinRepository coinRepository;
    private final WebClient upbitWebClient;
    private final ObjectMapper objectMapper;

    // POPULAR_COINS 리스트는 이제 필요 없습니다. 모든 KRW 마켓을 동적으로 가져올 것입니다.

    public CoinService(CoinRepository coinRepository, WebClient.Builder webClientBuilder) {
        this.coinRepository = coinRepository;
        this.upbitWebClient = webClientBuilder.baseUrl("https://api.upbit.com").build();
        this.objectMapper = new ObjectMapper();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialDataLoad() {
        System.out.println("Initial Upbit data load on application start at " + LocalDateTime.now());
        collectUpbitMarketData();
        System.out.println("Finished initial data load.");
    }

    public void collectUpbitMarketData() {
        System.out.println("Starting Upbit data collection for all KRW markets...");
        try {
            // 1. 모든 마켓 정보 가져오기 (한글명, 유의/주의 여부 포함)
            // isDetails=true를 통해 market_event 정보도 함께 가져옴
            List<Map<String, Object>> allMarkets = upbitWebClient.get()
                    .uri("/v1/market/all?isDetails=true")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();

            // KRW 마켓만 필터링하고 심볼 리스트를 생성
            List<String> krwMarketSymbols = allMarkets.stream()
                    .filter(m -> m.get("market") instanceof String && ((String) m.get("market")).startsWith("KRW-"))
                    .map(m -> (String) m.get("market"))
                    .collect(Collectors.toList());

            // 한글명 및 유의/주의 상세 정보를 심볼을 키로 하는 맵으로 변환하여 빠른 조회를 가능하게 함
            Map<String, Map<String, Object>> krwMarketDetailsMap = allMarkets.stream()
                    .filter(m -> krwMarketSymbols.contains((String) m.get("market"))) // KRW 마켓만 필터링
                    .collect(Collectors.toMap(m -> (String) m.get("market"), m -> m));

            // 2. 모든 KRW 마켓 코인들의 실시간 시세 정보 가져오기
            // Upbit API는 한 번에 너무 많은 심볼을 받을 수 없으므로, 심볼 리스트를 분할하여 요청합니다.
            // 여기서는 100개씩 끊어서 요청하는 예시를 보여줍니다. (Upbit API 제한에 따라 조정 필요)
            final int BATCH_SIZE = 100; // 한 번에 요청할 최대 심볼 개수
            for (int i = 0; i < krwMarketSymbols.size(); i += BATCH_SIZE) {
                List<String> batchSymbols = krwMarketSymbols.subList(i, Math.min(i + BATCH_SIZE, krwMarketSymbols.size()));
                String marketsParam = String.join(",", batchSymbols);

                List<Map<String, Object>> tickers = upbitWebClient.get()
                        .uri("/v1/ticker?markets={markets}", marketsParam)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                        .block(); // 블로킹 호출

                // 가져온 시세 데이터와 마켓 상세 정보를 결합하여 DB에 저장/업데이트
                for (Map<String, Object> ticker : tickers) {
                    String symbol = (String) ticker.get("market");
                    Optional<Coin> existingCoin = coinRepository.findBySymbol(symbol);
                    Coin coin = existingCoin.orElseGet(() -> Coin.builder().build());

                    coin.setSymbol(symbol);

                    // currentPrice는 엔티티에서 제거되었으므로, 해당 필드를 설정하는 코드는 제거됩니다.

                    if (ticker.get("signed_change_rate") != null) {
                        coin.setChangeRate(Float.parseFloat(ticker.get("signed_change_rate").toString()) * 100);
                    }
                    if (ticker.get("acc_trade_volume_24h") != null) {
                        coin.setTradeVolume(Float.parseFloat(ticker.get("acc_trade_volume_24h").toString()));
                    }

                    // market_all에서 가져온 상세 정보 (한글명, 유의/주의 여부) 설정
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

            System.out.println("Upbit data collection for all KRW markets completed successfully. Total coins collected: " + krwMarketSymbols.size());

        } catch (Exception e) {
            System.err.println("Error collecting Upbit market data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}