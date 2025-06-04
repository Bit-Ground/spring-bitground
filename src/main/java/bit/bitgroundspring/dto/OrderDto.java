package bit.bitgroundspring.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderDto {
    private String symbol;
    private String koreanName;
    private Double amount;
    private Double tradePrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String orderType;
}
