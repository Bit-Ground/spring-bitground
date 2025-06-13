package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.RankingDto;
import bit.bitgroundspring.dto.projection.RankProjection;
import bit.bitgroundspring.service.RankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/rank")
public class RankController {

    private final RankService rankService;
    
    /**
     * 현재 시즌 랭킹 조회
     */
    @GetMapping("/current")
    public ResponseEntity<List<RankProjection>> getCurrentRank() {
        return ResponseEntity.ok(rankService.getCurrentRankings());
    }

    /**
     * 특정 시즌 랭킹 조회
     */
    @GetMapping("/{seasonId}")
    public ResponseEntity<List<RankProjection>> getSeasonRank(@PathVariable Integer seasonId) {
        return ResponseEntity.ok(rankService.getSeasonRankings(seasonId));
    }

    //툴팁용
    @GetMapping("/current/detailed")
    public ResponseEntity<List<RankingDto>> getCurrentRankDetailed() {
        return ResponseEntity.ok(rankService.getCurrentRankingDtos());
    }
}
