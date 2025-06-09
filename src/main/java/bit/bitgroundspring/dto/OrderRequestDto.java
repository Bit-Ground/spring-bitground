package bit.bitgroundspring.dto;

import lombok.Data;

@Data
public class OrderRequestDto {
    private String symbol;
    private double amount;
    private Double limitPrice;
}