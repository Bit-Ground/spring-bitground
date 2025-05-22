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
}
