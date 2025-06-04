package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.projection.RankProjection;
import bit.bitgroundspring.repository.RankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RankService {

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
}
