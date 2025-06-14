package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.PastSeasonTierDto;
import bit.bitgroundspring.dto.RankingDto;
import bit.bitgroundspring.dto.projection.RankProjection;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.repository.RankRepository;
import bit.bitgroundspring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankService {

    private final UserRepository userRepository;
    private final RankRepository rankRepository;
    
    /**
     * 현 시즌 랭킹 조회
     */
    public List<RankProjection> getCurrentRankings() {
        return rankRepository.findCurrentSeasonRankings();
    }
    
    /**
     * 특정 시즌 랭킹 조회
     */
    public List<RankProjection> getSeasonRankings(int seasonId) {
        return rankRepository.findRankingsBySeasonId(seasonId);
    }

    // 새로운 메서드: DTO로 확장
    public List<RankingDto> getCurrentRankingDtos() {
        List<RankProjection> projections = rankRepository.findCurrentSeasonRankings();

        //  현재 시즌 이름 추출
        String currentSeasonName = projections.stream()
                .findFirst()
                .map(RankProjection::getSeasonName)
                .orElse(null);

        return projections.stream().map(proj -> {
            User user = userRepository.findById(proj.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + proj.getUserId()));

            //  현재 시즌 제외한 지난 시즌 5개 티어
            List<PastSeasonTierDto> pastSeasonTiers = rankRepository.findTop5ByUserWithSeason(user).stream()
                    .filter(r -> !r.getSeason().getName().equals(currentSeasonName))
                    .map(r -> new PastSeasonTierDto(r.getSeason().getName(), r.getTier()))
                    .limit(5)
                    .collect(Collectors.toList());

            List<Integer> pastTiers = rankRepository.findTop5ByUserOrderBySeasonIdDesc(user).stream()
                    .map(r -> r.getTier())
                    .collect(Collectors.toList());

            Integer highestTier = rankRepository.findHighestTierByUser(user);

            int initialCash = 10_000_000;
            int totalValue = proj.getTotalValue();
            int currentReturnRate = (int) (((double)(totalValue - initialCash) / initialCash) * 100);

            return RankingDto.builder()
                    .userId(proj.getUserId())
                    .seasonId(proj.getSeasonId())
                    .name(proj.getName())
                    .profileImage(proj.getProfileImage())
                    .ranks(proj.getRanks())
                    .tier(proj.getTier())
                    .totalValue(totalValue)
                    .pastTiers(pastTiers)
                    .highestTier(highestTier)
                    .currentReturnRate(currentReturnRate)
                    .pastSeasonTiers(pastSeasonTiers)
                    .updatedAt(proj.getUpdatedAt())
                    .currentSeasonName(currentSeasonName)
                    .build();
        }).collect(Collectors.toList());
    }
}
