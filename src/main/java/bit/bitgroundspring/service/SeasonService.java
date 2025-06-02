package bit.bitgroundspring.service;

import bit.bitgroundspring.entity.Season;
import bit.bitgroundspring.repository.SeasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SeasonService {

    private final SeasonRepository seasonRepository;

    /**
     * ✅ 현재 진행 중인 시즌의 ID를 반환
     * - 종료되지 않은 시즌 (endAt == null)을 조회
     * - 없을 경우 0을 반환
     *
     * @return 현재 시즌 ID 또는 0 (없을 경우)
     */
    public int getCurrentSeasonId() {
        Season season = seasonRepository.findFirstByEndAtIsNull();
        return (season != null) ? season.getId().intValue() : 0;  // null 방지!
    }
    


}
