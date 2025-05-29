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

@RestController // REST API 컨트롤러임을 명시
@RequestMapping("/api") // 모든 엔드포인트의 기본 경로 (예: /api/coins, /api/coins/high-volume)
public class CoinController {

    private final CoinRepository coinRepository; // Coin 데이터 접근을 위한 Repository
    private final GeminiService geminiService;

    // 생성자 주입
    public CoinController(CoinRepository coinRepository, GeminiService geminiService) {
        this.coinRepository = coinRepository;
        this.geminiService = geminiService;
    }

    // 모든 코인 정보 조회 엔드포인트
    // GET 요청: http://localhost:8090/api/coins
    @GetMapping("/coins")
    public List<Coin> getAllCoins() {
        return coinRepository.findAll(); // DB에서 모든 코인 데이터를 가져와 반환
    }

    @GetMapping("/coin-symbols")
    public List<CoinSymbolDto> getCoinSymbols() {
        return coinRepository.findAllSymbols();
    }
    // --- 수정된 부분: 거래대금 (tradePrice24h)으로 정렬 ---
    // 기존: getTop5HighVolumeCoins
    // 변경: getTop5HighTradePriceCoins
    // GET 요청: http://localhost:8090/api/coins/high-trade-price
    @GetMapping("/coins/high-trade-price") // 엔드포인트 이름도 변경
    public List<Coin> getTop5HighTradePriceCoins() {
        return coinRepository.findAll().stream()
                .filter(coin -> coin.getTradePrice24h() != null) // tradePrice24h가 null이 아닌 코인만 필터링
                .sorted(Comparator.comparing(Coin::getTradePrice24h, Comparator.reverseOrder())) // tradePrice24h 내림차순 정렬
                .limit(5)
                .collect(Collectors.toList());
    }

    // 상승폭 큰 종목 상위 5개 조회 엔드포인트
    // GET 요청: http://localhost:8090/api/coins/price-increase
    @GetMapping("/coins/price-increase")
    public List<Coin> getTop5PriceIncreaseCoins() {
        return coinRepository.findAll().stream()
                .filter(coin -> coin.getChangeRate() != null && coin.getChangeRate() > 0) // 변동률이 null이 아니고 양수인 코인만 필터링
                .sorted(Comparator.comparing(Coin::getChangeRate, Comparator.reverseOrder())) // 변동률 내림차순 정렬
                .limit(5)
                .collect(Collectors.toList());
    }

    // 하락폭 큰 종목 상위 5개 조회 엔드포인트
    // GET 요청: http://localhost:8090/api/coins/price-decrease
    @GetMapping("/coins/price-decrease")
    public List<Coin> getTop5PriceDecreaseCoins() {
        return coinRepository.findAll().stream()
                .filter(coin -> coin.getChangeRate() != null && coin.getChangeRate() < 0) // 변동률이 null이 아니고 음수인 코인만 필터링
                .sorted(Comparator.comparing(Coin::getChangeRate)) // 변동률 오름차순 정렬 (음수이므로 절대값이 큰 것이 먼저 오도록)
                .limit(5)
                .collect(Collectors.toList());
    }

    // 거래유의 종목 조회 엔드포인트 (Upbit의 'warning'에 해당)
    // GET 요청: http://localhost:8090/api/coins/caution
    @GetMapping("/coins/caution")
    public List<Coin> getCautionCoins() {
        return coinRepository.findAll().stream()
                .filter(Coin::getIsWarning) // isWarning 필드가 true인 코인만 필터링
                .collect(Collectors.toList());
    }

    // 투자주의 종목 조회 엔드포인트 (Upbit의 'caution'에 해당)
    // GET 요청: http://localhost:8090/api/coins/alert
    @GetMapping("/coins/alert")
    public List<Coin> getAlertCoins() {
        return coinRepository.findAll().stream()
                .filter(Coin::getIsCaution) // isCaution 필드가 true인 코인만 필터링
                .collect(Collectors.toList());
    }

    @GetMapping("/coins/{symbol}/insight") // 엔드포인트 경로를 /insight로 변경
    public ResponseEntity<AiInsight> getCoinInsight(@PathVariable String symbol) { // 메서드 이름도 getCoinInsight로 변경
        Optional<Coin> optionalCoin = coinRepository.findBySymbol(symbol);

        if (optionalCoin.isPresent()) {
            Coin coin = optionalCoin.get();

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

}