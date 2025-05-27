package bit.bitgroundspring.service;

import bit.bitgroundspring.entity.Season;
import bit.bitgroundspring.repository.SeasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service // 스프링이 이 클래스를 서비스 빈으로 등록
@RequiredArgsConstructor // final 필드에 대해 생성자 자동 생성 (DI 주입)
public class SeasonService {

    // 시즌 데이터를 DB와 연결해주는 JPA 인터페이스
    private final SeasonRepository seasonRepository;

    /**
     * 매일 자정 스케줄러가 호출하는 메서드.
     * 날짜에 따라 시즌을 자동으로 종료하거나 새 시즌을 시작함.
     * - 종료 조건: 매월 15일 또는 말일
     * - 시작 조건: 매월 1일 또는 16일
     */
    @Transactional
    public void handleFixedSeasonSchedule() {
        LocalDate today = LocalDate.now();
        int day = today.getDayOfMonth();         // 오늘 날짜 (예: 1~31)
        int lastDay = today.lengthOfMonth();     // 이번 달 말일

        // 아직 종료되지 않은(=진행 중인) 시즌 하나 조회
        Season currentSeason = seasonRepository.findFirstByEndAtIsNull();

        // 시즌 종료 조건 (15일 또는 말일)
        if (day == 15 || day == lastDay) {
            if (currentSeason != null) {
                endSeason(currentSeason);
            }
        }

        // 시즌 시작 조건 (1일 또는 16일)
        if (day == 1 || day == 16) {
            startNewSeason();
        }
    }

    /**
     * 새로운 시즌을 시작함.
     * - 이름: "Season N" 자동 부여
     * - 시작 시각: 현재 시간
     * - 종료일: null (아직 진행 중이므로)
     * - rewardCalculated: 정산 아직 안 됨
     */
    @Transactional
    public void startNewSeason() {
        Season season = Season.builder()
                .name("Season " + (seasonRepository.count() + 1)) // 시즌 번호 자동
                .startAt(LocalDateTime.now())                    // 현재 시작 시각
                .endAt(null)                                      // 종료 안 됨
                .rewardCalculated(false)                          // 정산 안 됨
                .build();

        seasonRepository.save(season); // DB에 저장
        System.out.println("[시즌 시작] " + season.getName());
    }

    /**
     * 현재 진행 중인 시즌을 종료 처리함.
     * - 종료 시각: 현재 시간
     * - rewardCalculated: true로 설정
     */
    @Transactional
    public void endSeason(Season season) {
        season.setEndAt(LocalDateTime.now());  // 종료 시각 설정
        season.setRewardCalculated(true);      // 정산 완료 표시
        seasonRepository.save(season);         // DB에 저장
        System.out.println("[시즌 종료] " + season.getName());
    }
}
