package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.RankingDto;
import bit.bitgroundspring.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rankings")
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/{seasonId}")
    public List<RankingDto> getLiveRankings(@PathVariable int seasonId) {
        return rankingService.getLiveRankingsBySeason(seasonId);
    }

    /**
     * 🔴 실시간 랭킹 (현재 시즌)
     */
//    @GetMapping("/live")
//    public List<RankingDto> getLiveRankings() {
//        Season current = seasonRepository.findFirstByEndAtIsNull();
//        return rankingService.getRankingsBySeason(current);
//    }
//
//    /**
//     * 🟢 종료된 시즌 랭킹
//     */
//    @GetMapping("/season/{seasonId}")
//    public List<RankingDto> getPastRanking(@PathVariable int seasonId) {
//        Season season = seasonRepository.findById((long) seasonId)
//                .orElseThrow(() -> new RuntimeException("시즌 없음"));
//
//        if (season.getEndAt() == null) {
//            throw new IllegalStateException("아직 종료되지 않은 시즌입니다.");
//        }
//
//        return rankingService.getRankingsBySeason(season);
//    }

}
