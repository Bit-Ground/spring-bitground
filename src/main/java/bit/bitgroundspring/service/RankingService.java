package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.RankingDto;
import bit.bitgroundspring.entity.Status;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.entity.UserAsset;
import bit.bitgroundspring.entity.UserRanking;
import bit.bitgroundspring.repository.RankingRepository;
import bit.bitgroundspring.repository.UserAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final RankingRepository rankingRepository;
    private final UserAssetRepository userAssetRepository;

    /**
     * ✔️ 전 시즌 랭킹 조회 (status = COMPLETED)
     * - 전 시즌 데이터 전체를 불러오고,
     * - 시즌 ID 기준으로 최신 시즌부터 정렬,
     * - total_value 기준으로 내림차순 정렬
     *
     * @return List<RankingDto> : 시즌별 유저 랭킹 정보 리스트
     */
    public List<RankingDto> getCompletedSeasonRankings() {
        List<UserRanking> rankings = rankingRepository
                .findBySeason_StatusOrderBySeason_IdDescTotalValueDesc(Status.COMPLETED);

        int[] rankCounter = {1}; // 순위 초기값

        return rankings.stream()
                .map(r -> RankingDto.builder()
                        .userId(r.getUser().getId())               // 유저 ID
                        .seasonId(r.getSeason().getId())           // 시즌 ID
                        .ranks(rankCounter[0]++)                   // 순위 (1부터 증가)
                        .totalValue(r.getTotalValue())             // 총 자산
                        .tier(r.getTier())                         // 티어
                        .profileImage(r.getUser().getProfileImage()) // 프로필 이미지
                        .build())
                .collect(Collectors.toList());

    }

    /**
     * ✔️ 실시간 랭킹 조회 (진행 중인 시즌 기준)
     * - 특정 시즌 ID를 받아 해당 시즌의 유저 랭킹을 자산 기준으로 정렬
     *
     * @param seasonId 시즌 ID
     * @return List<RankingDto> : 유저별 랭킹 정보
     */
    public List<RankingDto> getLiveRankingsBySeason(int seasonId) {
        List<UserRanking> rankings = rankingRepository.findWithUserBySeasonId(seasonId);

        int[] rankCounter = {1};

        return rankings.stream()
                .map(r -> {
                    // null 방어 처리
                    if (r == null || r.getUser() == null || r.getSeason() == null) return null;

                    return RankingDto.builder()
                            .ranks(rankCounter[0]++)
                            .userId(r.getUser().getId())
                            .seasonId(r.getSeason().getId())
                            .totalValue(r.getTotalValue())
                            .tier(r.getTier())
                            .profileImage(r.getUser().getProfileImage())
                            .build();
                })
                .filter(dto -> dto != null) // null 제거
                .collect(Collectors.toList());
    }

    /**
     * ✔️ 특정 유저의 보유 자산 총합 계산
     * - KRW-로 시작하는 마켓만 계산
     * - 코인 수량 * 시세를 누적합
     *
     * @param user User 엔터티
     * @param priceMap 현재 시세 map (ex. "KRW-BTC" -> 96000000f)
     * @return 총 자산 float
     */
    public float getUserTotalValue(User user, Map<String, Float> priceMap) {
        List<UserAsset> assets = userAssetRepository.findByUser(user);

        float totalValue = 0f;

        for (UserAsset asset : assets) {
            String symbol = asset.getCoin().getSymbol();

            // KRW 마켓만 계산
            if (!symbol.startsWith("KRW-")) continue;

            float quantity = asset.getAmount();                     // 보유 수량
            float price = priceMap.getOrDefault(symbol, 0f);        // 시세

            totalValue += quantity * price;                         // 자산 계산
        }

        return totalValue;
    }
}
