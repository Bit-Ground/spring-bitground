package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.OrderDto;
import bit.bitgroundspring.dto.projection.OrderProjection;
import bit.bitgroundspring.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public List<OrderDto> getOrdersBySeason(Integer seasonId, Integer userId) {
        List<OrderProjection> projections =
                orderRepository.findBySeasonIdAndUserId(Long.valueOf(seasonId), userId);

        return projections.stream()
                .map(p -> new OrderDto(
                        p.getSymbol(),
                        p.getCoinName(),
                        p.getAmount(),
                        p.getTradePrice(),
                        p.getCreatedAt(),
                        p.getUpdatedAt(),
                        p.getOrderType()
                ))
                .toList();
    }
}
