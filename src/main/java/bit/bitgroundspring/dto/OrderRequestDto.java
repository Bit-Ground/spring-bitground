package bit.bitgroundspring.dto;

import bit.bitgroundspring.entity.OrderType;
import lombok.Data;

@Data
public class OrderRequestDto {
    private String symbol;
    private double amount;
    private Double limitPrice;
    private OrderType orderType;
}