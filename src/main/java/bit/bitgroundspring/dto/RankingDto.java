package bit.bitgroundspring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
    private LocalDateTime updatedAt;

    //툴팁용 추가 필드
    private Integer currentReturnRate;         // 현재 수익률
    private Integer highestTier;               // 최고 티어
    private List<Integer> pastTiers;           // 지난 시즌 티어 리스트 (최대 5개)
    private String currentSeasonName; // ← 현재 시즌 이름 추가

    //추가 : 시즌 이름과 티어를 담은 리스트
    private List<PastSeasonTierDto> pastSeasonTiers;
}
