package bit.bitgroundspring.scheduler;

import bit.bitgroundspring.service.GbIndexService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class GbIndexScheduler {
    private final GbIndexService gbIndexService;

    public GbIndexScheduler(GbIndexService gbIndexService) {
        this.gbIndexService = gbIndexService;
    }

    // 매시간 정각에만 실행 (예외 방지용 정각 체크 포함)
    @Scheduled(cron = "0 0 * * * ?")
    public void saveGbIndexHourly() {
        LocalDateTime now = LocalDateTime.now();

        // 혹시라도 오차 발생 시 정각 아닌 경우는 무시
        if (now.getMinute() != 0 || now.getSecond() != 0) {
//            System.out.println("정각이 아니므로 저장하지 않음: " + now);
            return;
        }

//        System.out.println("GB 지수 저장 실행: " + now);
//        gbIndexService.saveGbIndexToDbAt(now.withMinute(0).withSecond(0).withNano(0));  // 정각 고정
    }
}
