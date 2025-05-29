package bit.bitgroundspring.controller;

import bit.bitgroundspring.service.GbIndexService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/gbindex")
public class GbIndexController {
    private final GbIndexService gbIndexService;

    public GbIndexController(GbIndexService gbIndexService) {
        this.gbIndexService = gbIndexService;
    }

    @GetMapping
    public Map<String, Double> getGbIndex() {
        return gbIndexService.calculateGbIndices();  // Map<String, Double>로 수정
    }
}
