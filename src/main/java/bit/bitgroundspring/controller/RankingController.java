package bit.bitgroundspring.controller;

import bit.bitgroundspring.service.RankingService;
import bit.bitgroundspring.service.SeasonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/rankings")
public class RankingController {

    private final RankingService rankingService;
    private final SeasonService seasonService;

  
}
