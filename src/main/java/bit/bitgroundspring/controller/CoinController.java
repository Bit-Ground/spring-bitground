// src/main/java/bit/bitgroundspring/controller/CoinController.java
package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.CoinSymbolDto;
import bit.bitgroundspring.entity.AiInsight;
import bit.bitgroundspring.entity.Coin;
import bit.bitgroundspring.repository.CoinRepository;
import bit.bitgroundspring.service.GeminiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CoinController {

    private final CoinRepository coinRepository;
    private final GeminiService geminiService;

    public CoinController(CoinRepository coinRepository, GeminiService geminiService) {
        this.coinRepository = coinRepository;
        this.geminiService = geminiService;
    }

    // 모든 코인 정보 조회 엔드포인트
    // GET 요청: http://localhost:8090/api/coins
    @GetMapping("/coins")
    public List<Coin> getAllCoins() {
        return coinRepository.findAll();
    }

    // 거래대금 상위 5개 코인 조회 엔드포인트
    // GET 요청: http://localhost:8090/api/coins/high-trade-price
    @GetMapping("/coins/high-trade-price")
    public List<Coin> getTop5HighTradePriceCoins() {
        return coinRepository.findAll().stream()
                .sorted(Comparator.comparing(Coin::getTradePrice24h, Comparator.reverseOrder()))
                .limit(5)
                .collect(Collectors.toList());
    }

    // 상승폭 큰 종목 상위 5개 코인 조회 엔드포인트
    // GET 요청: http://localhost:8090/api/coins/price-increase
    @GetMapping("/coins/price-increase")
    public List<Coin> getTop5PriceIncreaseCoins() {
        return coinRepository.findAll().stream()
                .sorted(Comparator.comparing(Coin::getChangeRate, Comparator.reverseOrder()))
                .limit(5)
                .collect(Collectors.toList());
    }

    // 하락폭 큰 종목 상위 5개 코인 조회 엔드포인트
    // GET 요청: http://localhost:8090/api/coins/price-decrease
    @GetMapping("/coins/price-decrease")
    public List<Coin> getTop5PriceDecreaseCoins() {
        return coinRepository.findAll().stream()
                .sorted(Comparator.comparing(Coin::getChangeRate)) // 오름차순 (가장 낮은 changeRate가 가장 큰 하락폭)
                .limit(5)
                .collect(Collectors.toList());
    }

    // 거래유의 종목 (isWarning = true) 코인 조회 엔드포인트 (Upbit의 'warning'에 해당)
    // GET 요청: http://localhost:8090/api/coins/caution
    @GetMapping("/coins/caution")
    public List<Coin> getCautionCoins() {
        return coinRepository.findAll().stream()
                .filter(Coin::getIsWarning) // isWarning 필드가 true인 코인만 필터링
                .collect(Collectors.toList());
    }

    // 투자주의 종목 (isCaution = true) 코인 조회 엔드포인트 (Upbit의 'caution'에 해당)
    // GET 요청: http://localhost:8090/api/coins/alert
    @GetMapping("/coins/alert")
    public List<Coin> getAlertCoins() {
        return coinRepository.findAll().stream()
                .filter(Coin::getIsCaution) // isCaution 필드가 true인 코인만 필터링
                .collect(Collectors.toList());
    }

    // GET /api/ai-insights/overall-market
    @GetMapping("/ai-insights/overall-market")
    public ResponseEntity<AiInsight> getOverallMarketInsight() {
        String marketOverallSymbol = "MARKET_OVERALL"; // GeminiService에 정의된 상수와 일치해야 함
        AiInsight aiInsight = geminiService.getTodayInsight(marketOverallSymbol);

        if (aiInsight != null) {
            return ResponseEntity.ok(aiInsight);
        } else {
            System.err.println("Failed to retrieve overall market AI insight for today.");
            // 데이터가 없을 경우 404 Not Found 또는 다른 적절한 상태 코드를 반환
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // 모든 코인 심볼 조회 엔드포인트 (DTO 사용)
    // GET 요청: http://localhost:8090/api/coins/symbols
    @GetMapping("/coins/symbols")
    public List<CoinSymbolDto> getCoinSymbols() {
        return coinRepository.findAll().stream()
                .map(coin -> new CoinSymbolDto(coin.getSymbol(), coin.getKoreanName()))
                .collect(Collectors.toList());
    }


    /**
     * 특정 코인 또는 전체 시장에 대한 AI 분석 결과를 조회하는 엔드포인트.
     * 이 엔드포인트는 AI API를 직접 호출하지 않고, 스케줄러에 의해 미리 저장된 데이터를 반환합니다.
     *
     * GET 요청: http://localhost:8090/api/coins/{symbol}/insight
     * {symbol} 에는 "MARKET_OVERALL" 또는 "KRW-BTC", "KRW-ETH" 등의 코인 심볼이 올 수 있습니다.
     */
    @GetMapping("/coins/{symbol}/insight")
    public ResponseEntity<AiInsight> getCoinInsight(@PathVariable String symbol) {
        AiInsight aiInsight = geminiService.getTodayInsight(symbol); // 스케줄러로 생성된 데이터 조회

        if (aiInsight != null) {
            return ResponseEntity.ok(aiInsight);
        } else {
            System.err.println("No AI insight found for symbol: " + symbol + " for today.");
            // 404 Not Found 또는 다른 적절한 상태 코드를 반환
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}