package bit.bitgroundspring.repository;

import bit.bitgroundspring.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeasonRepository extends JpaRepository<Season, Long> {
    /**
     * ✅ 현재 진행 중인 시즌을 하나 조회하는 메서드
     * - 조건: 종료일(endAt)이 null인 시즌 = 아직 끝나지 않은 시즌
     * - 결과: 진행 중인 시즌이 하나만 존재한다고 가정하고, 그 시즌을 가져옴
     * - 사용처: 실시간 랭킹 조회, 시즌 시작/종료 스케줄러 등
     *
     *
     * @return 아직 종료되지 않은 시즌 (없으면 null 반환)
     */
    Season findFirstByEndAtIsNull();
}
