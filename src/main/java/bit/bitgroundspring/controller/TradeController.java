package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.OrderRequestDto;
import bit.bitgroundspring.dto.TradeDto;
import bit.bitgroundspring.dto.response.OrderResponseDto;
import bit.bitgroundspring.security.oauth2.AuthService;
import bit.bitgroundspring.service.OrderService;
import bit.bitgroundspring.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trade")
@RequiredArgsConstructor
public class TradeController {
    private final OrderService orderService;
    private final TradeService tradeService;
    private final AuthService authService;

    //    @GetMapping("/history")
//    public ResponseEntity<List<TradeDto>> getTradeHistory(
//            @RequestParam("symbol") String symbol,
//            @RequestParam(value = "since", required = false) String sinceStr
//    ) {
//        if (sinceStr == null) {
//            // since 파라미터가 없으면 최신 100개 전체 조회
//            List<TradeDto> history = orderService.getRecentTrades(symbol);
//            return ResponseEntity.ok(history);
//        }
//        // since 파라미터가 있으면 그 시간 이후의 변경분만 조회
//        LocalDateTime since = LocalDateTime.parse(sinceStr);
//        List<TradeDto> newTrades = orderService.getNewTradesSince(symbol, since);
//        return ResponseEntity.ok(newTrades);
//    }
    @GetMapping("/history")
    public ResponseEntity<List<TradeDto>> getTradeHistory(
            @RequestParam("symbol") String symbol
    ) {
        List<TradeDto> history = orderService.getRecentTrades(symbol);
        return ResponseEntity.ok(history);
    }

    @PostMapping
    public ResponseEntity<OrderResponseDto> placeOrder(
            @CookieValue (value = "jwt_token", required = false) String jwtToken,
            @RequestBody OrderRequestDto req
    ) {
        Integer userId = authService.getUserIdFromToken(jwtToken);
        OrderResponseDto resp = tradeService.placeOrder(userId, req);
        return ResponseEntity.ok(resp);
    }
}
