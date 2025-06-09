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
                .sorted(Comparator.comparing(Coin::getTradePrice24h).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    // 상승폭 큰 종목 상위 5개 코인 조회 엔드포인트
    // GET 요청: http://localhost:8090/api/coins/price-increase
    @GetMapping("/coins/price-increase")
    public List<Coin> getTop5PriceIncreaseCoins() {
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
        return coinRepository.findAll().stream()
                .filter(Coin::getIsWarning)
                .collect(Collectors.toList());
    }

    // 투자주의 종목 조회 엔드포인트 (Upbit의 'caution'에 해당)
    // GET 요청: http://localhost:8090/api/coins/alert
    @GetMapping("/coins/alert")
    public List<Coin> getAlertCoins() {
        return coinRepository.findAll().stream()
                .filter(Coin::getIsCaution)
                .collect(Collectors.toList());
    }

    // 모든 코인 심볼 조회 엔드포인트 (DTO 사용)
    // GET 요청: http://localhost:8090/api/coins/symbols
    @GetMapping("/coins/symbols")
    public List<CoinSymbolDto> getCoinSymbols() {
        return coinRepository.findAll().stream()
                .map(coin -> new CoinSymbolDto(coin.getSymbol(), coin.getKoreanName()))
                .collect(Collectors.toList());
    }

    // 특정 코인에 대한 AI 분석 결과 조회 엔드포인트
    // GET 요청: http://localhost:8090/api/coins/{symbol}/insight
    @GetMapping("/coins/{symbol}/insight")
    public ResponseEntity<AiInsight> getCoinInsight(@PathVariable String symbol) {
        // 'MARKET_OVERALL' 심볼은 코인 저장소에 없으므로 별도로 처리
        if (symbol.equals(GeminiService.MARKET_OVERALL_SYMBOL)) {
            AiInsight aiInsight = geminiService.getTodayInsight(symbol);
            if (aiInsight != null) {
                return ResponseEntity.ok(aiInsight);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        }

        // 일반 코인 심볼 처리
        Optional<Coin> optionalCoin = coinRepository.findBySymbol(symbol);

        if (optionalCoin.isPresent()) {
            Coin coin = optionalCoin.get();
            // 해당 코인에 대한 AI 분석 결과를 가져오거나 생성하여 저장
            AiInsight aiInsight = geminiService.generateAndSaveAnalysis(coin);
            if (aiInsight != null) {
                return ResponseEntity.ok(aiInsight);
            } else {
                System.err.println("Failed to generate or retrieve AI insight for " + symbol);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        } else {
            System.err.println("Coin not found for symbol: " + symbol);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // 전체 시장에 대한 AI 분석 결과 조회 엔드포인트 (새로 추가)
    // 이 엔드포인트는 스케줄러가 'MARKET_OVERALL' 심볼로 저장한 데이터를 가져오는 데 사용됩니다.
    // GET 요청: http://localhost:8090/api/ai-insights/overall-market
    @GetMapping("/ai-insights/overall-market")
    public ResponseEntity<AiInsight> getOverallMarketInsight() {
        AiInsight aiInsight = geminiService.getTodayInsight(GeminiService.MARKET_OVERALL_SYMBOL);

        if (aiInsight != null) {
            return ResponseEntity.ok(aiInsight);
        } else {
            System.err.println("Failed to retrieve overall market AI insight for today.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}