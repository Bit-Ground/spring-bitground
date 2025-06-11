package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.UserAssetDto;
import bit.bitgroundspring.dto.response.UserAssetResponse;
import bit.bitgroundspring.entity.UserAsset;
import bit.bitgroundspring.security.oauth2.AuthService;
import bit.bitgroundspring.service.UserAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/assets")
@RequiredArgsConstructor
public class UserAssetController {
    private final AuthService authService;
    private final UserAssetService assetService;
    
    /**
     * 보유 코인 심볼 목록 조회
     **/
    @GetMapping("/owned")
    public ResponseEntity<List<String>> listOwned(
            @CookieValue (value = "jwt_token", required = false) String jwtToken) {
        // JWT 토큰에서 사용자 ID 추출
        Integer userId = authService.getUserIdFromToken(jwtToken);
        
        List<String> symbols = assetService.listOwnedSymbols(userId);
        return ResponseEntity.ok(symbols);
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<UserAssetDto> getAsset(
            @CookieValue(value="jwt_token", required=false) String jwtToken,
            @PathVariable String symbol
    ) {
        Integer userId = authService.getUserIdFromToken(jwtToken);

        Optional<UserAsset> opt = assetService.findByUserAndCoin(userId, symbol);
        UserAssetDto dto = opt
                .map(a -> new UserAssetDto(symbol, a.getAmount(), a.getAvgPrice()))
                .orElse(new UserAssetDto(symbol, 0000000f, 0000000f));

        return ResponseEntity.ok(dto);
    }
    
    /**
     * 보유 자산들 조회
     */
    @GetMapping
    public ResponseEntity<UserAssetResponse> getUserAssets(
            @CookieValue (value = "jwt_token", required = false) String jwtToken) {
        // JWT 토큰에서 사용자 ID 추출
        Integer userId = authService.getUserIdFromToken(jwtToken);
        
        UserAssetResponse response = assetService.getUserAssets(userId);
        return ResponseEntity.ok(response);
    }
}