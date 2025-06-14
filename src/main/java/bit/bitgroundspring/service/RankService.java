package bit.bitgroundspring.service;

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

        return projections.stream().map(proj -> {
            User user = userRepository.findById(proj.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + proj.getUserId()));

            // 🔹 지난 시즌 티어 5개
            List<Integer> pastTiers = rankRepository.findTop5ByUserOrderBySeasonIdDesc(user).stream()
                    .map(r -> r.getTier())
                    .collect(Collectors.toList());

            // 🔹 최고 티어
            Integer highestTier = rankRepository.findHighestTierByUser(user);

            // 🔹 현재 수익률 계산
            int initialCash = 10_000_000;
            int totalValue = proj.getTotalValue();
            int currentReturnRate = (int) (((double)(totalValue - initialCash) / initialCash) * 100);

            return RankingDto.builder()
                    .userId(proj.getUserId())
                    .seasonId(proj.getSeasonId()) // Projection에 포함돼 있어야 함
                    .name(proj.getName())
                    .profileImage(proj.getProfileImage())
                    .ranks(proj.getRanks())
                    .tier(proj.getTier())
                    .totalValue(totalValue)
                    .pastTiers(pastTiers)
                    .highestTier(highestTier)
                    .currentReturnRate(currentReturnRate)
                    .build();

        }).collect(Collectors.toList());
    }
}
