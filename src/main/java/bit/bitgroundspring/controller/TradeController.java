package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.TradeDto;
import bit.bitgroundspring.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/trade")
public class TradeController {
    @Autowired
    OrderService orderService;

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
}
