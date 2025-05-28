package bit.bitgroundspring.scheduler;

import bit.bitgroundspring.service.RankingService;
import bit.bitgroundspring.service.SeasonService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RankingScheduler {

    private final RankingService rankingService;
    private final SeasonService seasonService;

    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정
    public void scheduleSeason() {
        seasonService.handleFixedSeasonSchedule(); // 시즌 시작/종료 자동 처리
    }

}
