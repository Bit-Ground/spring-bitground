package bit.bitgroundspring.controller;

import bit.bitgroundspring.entity.Coin;
import bit.bitgroundspring.repository.CoinRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CoinController {

    private final CoinRepository coinRepository;

    public CoinController(CoinRepository coinRepository) {
        this.coinRepository = coinRepository;
    }

    @GetMapping("/coins")
    public List<Coin> getAllCoins() {
        return coinRepository.findAll();
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

    @GetMapping("/coins/price-increase")
    public List<Coin> getTop5PriceIncreaseCoins() {
        return coinRepository.findAll().stream()
                .filter(coin -> coin.getChangeRate() != null && coin.getChangeRate() > 0)
                .sorted(Comparator.comparing(Coin::getChangeRate, Comparator.reverseOrder()))
                .limit(5)
                .collect(Collectors.toList());
    }

    @GetMapping("/coins/price-decrease")
    public List<Coin> getTop5PriceDecreaseCoins() {
        return coinRepository.findAll().stream()
                .filter(coin -> coin.getChangeRate() != null && coin.getChangeRate() < 0)
                .sorted(Comparator.comparing(Coin::getChangeRate))
                .limit(5)
                .collect(Collectors.toList());
    }

    @GetMapping("/coins/caution")
    public List<Coin> getCautionCoins() {
        return coinRepository.findAll().stream()
                .filter(Coin::getIsWarning)
                .collect(Collectors.toList());
    }

    @GetMapping("/coins/alert")
    public List<Coin> getAlertCoins() {
        return coinRepository.findAll().stream()
                .filter(Coin::getIsCaution)
                .collect(Collectors.toList());
    }
}