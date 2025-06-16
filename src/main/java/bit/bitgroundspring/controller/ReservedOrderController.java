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
        createOrderRequest.setUserId(userId);
        // 예약 주문 처리
        orderService.createReserveOrder(createOrderRequest);
        
        return ResponseEntity.ok().build();
    }

    // ✅ 미체결(예약) 주문 조회
    @GetMapping
    public ResponseEntity<?> getPendingReserveOrders(
            @CookieValue(value = "jwt_token", required = false) String jwtToken
    ) {
        Integer userId = authService.getUserIdFromToken(jwtToken);
        return ResponseEntity.ok(orderService.getPendingOrdersByUserId(userId));
    }

//    삭제
@DeleteMapping("/{orderId}")
public ResponseEntity<?> cancelReservedOrder(
        @PathVariable Integer orderId,
        @CookieValue(value = "jwt_token", required = false) String jwtToken) {

    Integer userId = authService.getUserIdFromToken(jwtToken);
    orderService.cancelOrder(orderId, userId);
    return ResponseEntity.ok().build();
}

}
