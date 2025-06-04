package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.projection.OrderProjection;
import bit.bitgroundspring.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public List<OrderProjection> getOrdersBySeason(Integer seasonId, Integer userId) {
        return orderRepository.findBySeasonIdAndUserId(seasonId, userId);
    }
}
