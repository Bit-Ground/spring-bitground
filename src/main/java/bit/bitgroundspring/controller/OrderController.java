package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.projection.OrderProjection;
import bit.bitgroundspring.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import bit.bitgroundspring.security.oauth2.AuthService;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final AuthService authService;

    @GetMapping("/{seasonId}")
    public List<OrderProjection> getOrders(
            @PathVariable Integer seasonId,
            @CookieValue("jwt_token") String jwtToken) {

        Integer userId = authService.getUserIdFromToken(jwtToken); // ✅ 로그인한 사용자만
        return orderService.getOrdersBySeason(seasonId, userId);
    }



}
