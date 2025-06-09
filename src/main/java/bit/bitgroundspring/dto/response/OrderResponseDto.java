package bit.bitgroundspring.dto.response;

import bit.bitgroundspring.entity.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class OrderResponseDto {
    private Integer orderId;
    private String symbol;
    private String koreanName;
    private OrderType orderType;
    private Float amount;
    private Float tradePrice;
    private LocalDateTime createdAt;
}
