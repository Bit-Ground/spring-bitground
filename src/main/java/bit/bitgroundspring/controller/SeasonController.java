package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.projection.SeasonProjection;
import bit.bitgroundspring.service.SeasonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/seasons")
@RequiredArgsConstructor
public class SeasonController {
    private final SeasonService seasonService;
    
    /**
     * 모든 시즌 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<SeasonProjection>> getLatestSeasons() {
        return ResponseEntity.ok(seasonService.getLatestSeasons());
    }

    /**
     * 현재 진행 중인 시즌 ID만 반환
     */
    @GetMapping("/current-id")
    public ResponseEntity<Integer> getCurrentSeasonId() {
        Integer currentId = seasonService.getCurrentSeasonId();
        return ResponseEntity.ok(currentId);
    }
    
}
