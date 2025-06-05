package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.OrderDto;
import bit.bitgroundspring.dto.TradeDto;
import bit.bitgroundspring.dto.projection.OrderProjection;
import bit.bitgroundspring.entity.Status;
import bit.bitgroundspring.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public List<OrderProjection> getOrdersBySeason(Integer seasonId, Integer userId) {
        return orderRepository.findBySeasonIdAndUserId(seasonId, userId);
    }

    public List<TradeDto> getRecentTrades(String symbol) {
        return orderRepository.findTop30ByCoinSymbolAndStatus(symbol, Status.COMPLETED);
    }

    // 이후 변경분만
    public List<TradeDto> getNewTradesSince(String symbol, LocalDateTime since) {
        return orderRepository.findNewTradesByCoinSymbolAndStatusSince(symbol, Status.COMPLETED, since);
    }
}
