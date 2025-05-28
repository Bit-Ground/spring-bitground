package bit.bitgroundspring.scheduler;

import bit.bitgroundspring.service.CoinService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CoinScheduler {

    private final CoinService coinService;

    public CoinScheduler(CoinService coinService) {
        this.coinService = coinService;
    }

    // 매일 자정(00시 00분 00초)에 실행
    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduleCoinDataCollection() {
        // 스케줄러가 시작되었음을 알리는 로그는 유지하는 것이 좋습니다.
        System.out.println("Coin data collection scheduled task initiated at " + LocalDateTime.now());
        coinService.collectAndSaveAllKrwCoins();
        System.out.println("Coin data collection scheduled task completed."); // 완료 로그 추가 (선택 사항)
    }

//    // 테스트용 스케줄러는 제거합니다.
//     @Scheduled(fixedDelay = 10000, initialDelay = 10000)
//     public void runOnceOnStartupForTest() {
//          System.out.println("TEST: Coin data collection on startup initiated at " + LocalDateTime.now());
//          coinService.collectAndSaveAllKrwCoins();
//     }
}