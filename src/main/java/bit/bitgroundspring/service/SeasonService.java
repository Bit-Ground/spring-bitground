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

    /**
     * ✅ 새로운 시즌을 시작하는 메서드
     * - 이름은 "Season N" 형식으로 자동 생성
     * - 시작일은 오늘 날짜
     * - endAt은 null로 (아직 진행 중 상태)
     * - rewardCalculated는 false로 초기화
     */
    @Transactional
    public void startNewSeason() {
        Season season = Season.builder()
                .name("시즌 " + (seasonRepository.count() + 1)) // 시즌 이름 자동 증가
                .startAt(LocalDate.now())                         // 시작일은 오늘
                .endAt(null)                                      // 아직 종료 안 됨
                .rewardCalculated(false)                          // 보상 아직 계산 안 됨
                .build();

        seasonRepository.save(season);
        System.out.println("[시즌 시작] " + season.getName());
    }

    /**
     * ✅ 현재 시즌을 종료 처리
     * - 종료일을 오늘 날짜로 설정
     * - 보상 계산 완료 여부를 true로 설정
     *
     * @param season 종료할 시즌 엔티티
     */
    @Transactional
    public void endSeason(Season season) {
        season.setEndAt(LocalDate.now());     // 종료일 = 오늘
        season.setRewardCalculated(true);     // 보상 계산 완료 표시
        seasonRepository.save(season);
        System.out.println("[시즌 종료] " + season.getName());
    }

    /**
     * ✅ 매일 자정에 실행되는 시즌 스케줄러
     * - 시즌 종료 조건: 매월 15일 또는 말일
     * - 시즌 시작 조건: 매월 1일 또는 16일
     * - 자동으로 시즌을 종료하고, 필요한 경우 새 시즌을 시작함
     */
    @Transactional
    public void handleFixedSeasonSchedule() {
        LocalDate today = LocalDate.now();         // 오늘 날짜
        int day = today.getDayOfMonth();           // 오늘이 며칠인지
        int lastDay = today.lengthOfMonth();       // 이번 달 마지막 날

        // 현재 진행 중인 시즌 조회 (종료일이 아직 없는 것)
        Season currentSeason = seasonRepository.findFirstByEndAtIsNull();

        // 종료 조건: 15일 또는 말일
        if ((day == 15 || day == lastDay) && currentSeason != null) {
            endSeason(currentSeason);
        }

        // 시작 조건: 1일 또는 16일
        if (day == 1 || day == 16) {
            startNewSeason();
        }
    }
}
