package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.RankingDto;
import bit.bitgroundspring.service.RankingService;
import bit.bitgroundspring.service.SeasonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rankings")
public class RankingController {

    private final RankingService rankingService;
    private final SeasonService seasonService;

    /**
     * ✅ 실시간 랭킹 조회
     * - 주로 내부 전용 호출
     * - 시즌 ID 기준으로 실시간 랭킹 바로 반환
     */
    @GetMapping("/{seasonId}")
    public List<RankingDto> getLiveRankings(@PathVariable int seasonId) {
        return rankingService.getLiveRankingsBySeason(seasonId);
    }

    /**
     * ✅ 시즌 랭킹 조회 (실시간 또는 전시즌 자동 분기)
     * - 현재 시즌 ID와 비교해서
     *   - 현재 시즌이면 실시간 랭킹 반환
     *   - 과거 시즌이면 완료된 시즌 중 해당 시즌의 랭킹만 필터링하여 반환
     */
    @GetMapping("/season/{seasonId}")
    public List<RankingDto> getRankingsBySeason(@PathVariable int seasonId) {
        int currentSeasonId = seasonService.getCurrentSeasonId();

        // 전 시즌 랭킹 조회
        if (currentSeasonId == 0 || seasonId != currentSeasonId) {
            return rankingService.getCompletedSeasonRankings().stream()
                    .filter(r -> r.getSeasonId() == seasonId)// 해당 시즌만 추출
                    .toList();
        }
        // 현재 시즌이면 실시간 랭킹 조회
        return rankingService.getLiveRankingsBySeason(seasonId);
    }
}
