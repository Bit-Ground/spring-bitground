package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.response.UserAssetResponse;
import bit.bitgroundspring.security.oauth2.AuthService;
import bit.bitgroundspring.service.UserAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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