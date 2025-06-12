// src/main/java/bit/bitgroundspring/controller/CoinController.java

package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.CoinSymbolDto;
import bit.bitgroundspring.entity.AiInsight;
import bit.bitgroundspring.entity.Coin;
import bit.bitgroundspring.repository.CoinRepository;
import bit.bitgroundspring.service.GeminiService;
import lombok.RequiredArgsConstructor; // Lombok RequiredArgsConstructor 임포트
import lombok.extern.slf4j.Slf4j; // Lombok Slf4j 임포트
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Optional; // Optional 임포트 추가 (사용하지 않더라도 컴파일 에러 방지)
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 생성
@Slf4j // 로깅 사용
public class CoinController {

    private final CoinRepository coinRepository;
    private final GeminiService geminiService;

    // 모든 코인 정보 조회 엔드포인트
    // GET 요청: http://localhost:8090/api/coins
    @GetMapping("/coins")
    public List<Coin> getAllCoins() {
        log.info("Request to get all coins.");
        return coinRepository.findAll();
    }

    // 거래대금 상위 5개 코인 조회 엔드포인트
    // GET 요청: http://localhost:8090/api/coins/high-trade-price
    @GetMapping("/coins/high-trade-price")
    public List<Coin> getTop5HighTradePriceCoins() {
        log.info("Request to get top 5 coins by high trade price.");
        return coinRepository.findAll().stream()
                .sorted(Comparator.comparing(Coin::getTradePrice24h).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    // 상승폭 큰 종목 상위 5개 코인 조회 엔드포인트
    // GET 요청: http://localhost:8090/api/coins/price-increase
    @GetMapping("/coins/price-increase")
    public List<Coin> getTop5PriceIncreaseCoins() {
        log.info("Request to get top 5 coins by price increase.");
        return coinRepository.findAll().stream()
                .filter(coin -> coin.getChangeRate() != null)
                .sorted(Comparator.comparing(Coin::getChangeRate).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    // 하락폭 큰 종목 상위 5개 코인 조회 엔드포인트
    // GET 요청: http://localhost:8090/api/coins/price-decrease
    @GetMapping("/coins/price-decrease")
    public List<Coin> getTop5PriceDecreaseCoins() {
        log.info("Request to get top 5 coins by price decrease.");
        return coinRepository.findAll().stream()
                .filter(coin -> coin.getChangeRate() != null)
                .sorted(Comparator.comparing(Coin::getChangeRate))
                .limit(5)
                .collect(Collectors.toList());
    }

    // 거래유의 종목 조회 엔드포인트 (Upbit의 'warning'에 해당)
    // GET 요청: http://localhost:8090/api/coins/caution
    @GetMapping("/coins/caution")
    public List<Coin> getCautionCoins() {
        log.info("Request to get caution coins (isWarning).");
        return coinRepository.findAll().stream()
                .filter(Coin::getIsWarning)
                .collect(Collectors.toList());
    }

    // 투자주의 종목 조회 엔드포인트 (Upbit의 'caution'에 해당)
    // GET 요청: http://localhost:8090/api/coins/alert
    @GetMapping("/coins/alert")
    public List<Coin> getAlertCoins() {
        log.info("Request to get alert coins (isCaution).");
        return coinRepository.findAll().stream()
                .filter(Coin::getIsCaution)
                .collect(Collectors.toList());
    }

    // 모든 코인 심볼 조회 엔드포인트 (DTO 사용)
    // GET 요청: http://localhost:8090/api/coins/symbols
    @GetMapping("/coins/symbols")
    public List<CoinSymbolDto> getCoinSymbols() {
        log.info("Request to get all coin symbols.");
        return coinRepository.findAll().stream()
                .map(coin -> new CoinSymbolDto(coin.getSymbol(), coin.getKoreanName()))
                .collect(Collectors.toList());
    }

    /**
     * 특정 코인 또는 전체 시장에 대한 AI 분석 결과를 조회하는 엔드포인트입니다.
     * Go 서비스에 의해 DB에 미리 생성되어 저장된 데이터를 조회합니다.
     * GET 요청: http://localhost:8090/api/coins/{symbol}/insight
     *
     * @param symbol 조회할 코인 또는 시장의 심볼 (예: "KRW-BTC" 또는 "MARKET_OVERALL")
     * @return AI 분석 데이터를 담은 AiInsight 객체
     */
    @GetMapping("/coins/{symbol}/insight")
    public ResponseEntity<AiInsight> getCoinInsight(@PathVariable String symbol) {
        log.info("Request to get AI insight for symbol: {}", symbol);

        // Go 서비스가 이미 AI 인사이트를 생성하여 DB에 저장했으므로,
        // Spring 서비스는 단순히 조회만 수행합니다.
        AiInsight aiInsight = geminiService.getTodayInsight(symbol);

        if (aiInsight != null) {
            log.info("Successfully retrieved AI insight for symbol: {}. Insight ID: {}", symbol, aiInsight.getId());
            return ResponseEntity.ok(aiInsight);
        } else {
            log.warn("AI insight not found for symbol: {} today. Returning NOT_FOUND.", symbol);
            // AI 인사이트가 없는 경우 404 Not Found 또는 다른 적절한 상태 코드를 반환합니다.
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * 전체 암호화폐 시장에 대한 AI 분석 결과를 조회하는 엔드포인트입니다.
     * 이 엔드포인트는 Go 서비스가 'MARKET_OVERALL' 심볼로 저장한 데이터를 가져오는 데 사용됩니다.
     * GET 요청: http://localhost:8090/api/ai-insights/overall-market
     *
     * @return 전체 시장 AI 분석 데이터를 담은 AiInsight 객체
     */
    @GetMapping("/ai-insights/overall-market")
    public ResponseEntity<AiInsight> getOverallMarketInsight() {
        log.info("Request to get overall market AI insight.");

        // 'MARKET_OVERALL' 심볼은 Go 서비스가 전체 시장 분석을 저장할 때 사용하는 약속된 심볼입니다.
        // GeminiService에 상수가 없으므로, 직접 문자열 "MARKET_OVERALL"을 사용합니다.
        AiInsight aiInsight = geminiService.getTodayInsight("MARKET_OVERALL");

        if (aiInsight != null) {
            log.info("Successfully retrieved overall market AI insight for today. Insight ID: {}", aiInsight.getId());
            return ResponseEntity.ok(aiInsight);
        } else {
            log.warn("Overall market AI insight not found for today. Returning NOT_FOUND.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}
