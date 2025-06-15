package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.CreateOrderRequest;
import bit.bitgroundspring.security.oauth2.AuthService;
import bit.bitgroundspring.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trade/reserve")
@RequiredArgsConstructor
public class ReservedOrderController {
    
    private final OrderService orderService;
    private final AuthService authService;
    
    @PostMapping
    public ResponseEntity<?> placeReservedOrder(
            @CookieValue(value = "jwt_token", required = false) String jwtToken,
            @RequestBody CreateOrderRequest createOrderRequest
            ) {
        // JWT 토큰에서 사용자 ID 추출
        Integer userId = authService.getUserIdFromToken(jwtToken);
        System.out.println(createOrderRequest.getReservePrice());
        createOrderRequest.setUserId(userId);
        // 예약 주문 처리
        orderService.createReserveOrder(createOrderRequest);
        
        return ResponseEntity.ok().build();
    }
}
