package bit.bitgroundspring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 랭킹 정보를 클라이언트에게 전달하기 위한 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingDto {

    private String name;          // 유저 name
    private Integer userId;       // 유저 ID
    private Integer seasonId;     // 시즌 ID
    private Integer ranks;        // 순위
    private Integer totalValue;   // 총 자산
    private Integer tier;         // 티어 (0~4)
    private String profileImage; //프로필 이미지

}
