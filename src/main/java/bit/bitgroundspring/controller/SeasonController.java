package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.response.SeasonResponse;
import bit.bitgroundspring.service.SeasonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seasons")
@RequiredArgsConstructor
public class SeasonController {
    private final SeasonService seasonService;
    
    /**
     * 모든 시즌 목록 조회
     */
    @GetMapping
    public ResponseEntity<SeasonResponse> getLatestSeasons() {
        return ResponseEntity.ok(seasonService.getLatestSeasons());
    }
    
}
