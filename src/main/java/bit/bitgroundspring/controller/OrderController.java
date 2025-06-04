package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.OrderDto;
import bit.bitgroundspring.dto.response.UserAssetsResponse;
import bit.bitgroundspring.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import bit.bitgroundspring.security.oauth2.AuthService;
import bit.bitgroundspring.service.UserAssetService;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final AuthService authService;
    private final UserAssetService assetService;

    @GetMapping("/{seasonId}")
    public List<OrderDto> getOrders(
            @PathVariable Integer seasonId,
            @CookieValue("jwt_token") String jwtToken) {

        Integer userId = authService.getUserIdFromToken(jwtToken); // ✅ 로그인한 사용자만
        return orderService.getOrdersBySeason(seasonId, userId);
    }

    @GetMapping
    public ResponseEntity<UserAssetsResponse> getUserAssets(
            @CookieValue (value = "jwt_token", required = false) String jwtToken) {
        // JWT 토큰에서 사용자 ID 추출
        Integer userId = authService.getUserIdFromToken(jwtToken);

        UserAssetsResponse response = assetService.getUserAssets(userId);
        return ResponseEntity.ok(response);
    }

}
