package bit.bitgroundspring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PastSeasonTierDto {
    private String seasonName; // 예: "2024 시즌 1"
    private Integer tier;      // 예: 4
}