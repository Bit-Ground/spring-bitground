package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.RankingDto;
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
     * 실시간 랭킹 조회 - 자산 내림차순 정렬 후 순위 계산
     *
     * @param seasonId 조회할 시즌 ID
     * @return RankingDto 리스트 (랭킹 포함)
     */
    public List<RankingDto> getLiveRankingsBySeason(int seasonId) {
        List<UserRanking> rankings = rankingRepository.findBySeason_IdOrderByTotalValueDesc(seasonId);

        int[] rankCounter = {1};

        return rankings.stream()
                .map(r -> {
                    RankingDto dto = new RankingDto();
                    dto.setUserId(r.getUserId().getId());  // userId라는 이름의 User 객체에서 진짜 ID를 추출
                    dto.setSeasonId(r.getSeason().getId().intValue()); //Long 타입을 int로 변환해서 DTO에 넣음
                    dto.setRanks(rankCounter[0]++); // 1부터 순위 증가
                    dto.setTotalValue(r.getTotalValue()); // 총 자산 설정
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 유저의 총 자산 계산 (KRW 마켓 기준)
     *
     * @param user     자산을 계산할 유저
     * @param priceMap 시세 정보 (예: "KRW-BTC" -> 96000000f)
     * @return 총 자산 (float)
     */
    public float getUserTotalValue(User user, Map<String, Float> priceMap) {
        List<UserAsset> assets = userAssetRepository.findByUser(user);

        float totalValue = 0f;

        for (UserAsset asset : assets) {
            String market = asset.getId().getMarket();

            if (!market.startsWith("KRW-")) continue;

            float quantity = asset.getQuantity();
            float price = priceMap.getOrDefault(market, 0f);

            totalValue += quantity * price;
        }

        return totalValue;
    }

}
