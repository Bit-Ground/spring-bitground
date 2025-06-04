package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.projection.SeasonProjection;
import bit.bitgroundspring.repository.SeasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class SeasonService {

    private final SeasonRepository seasonRepository;
    
    /**
     * 시즌 목록을 내림차순으로 조회 (최신 시즌부터 최대 48개)
     */
    public List<SeasonProjection> getLatestSeasons() {
        return seasonRepository.findTop48ByOrderByIdDesc();
    }
    


}
