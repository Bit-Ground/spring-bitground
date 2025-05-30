package bit.bitgroundspring.controller;

import bit.bitgroundspring.service.UserAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class UserAssetController {
    private final UserAssetService assetService;

    /** 보유 코인 심볼 목록 조회 **/
    @GetMapping
    public ResponseEntity<List<String>> listOwned(
            @RequestParam("userId") Integer userId
    ) {
        List<String> symbols = assetService.listOwnedSymbols(userId);
        return ResponseEntity.ok(symbols);
    }
}