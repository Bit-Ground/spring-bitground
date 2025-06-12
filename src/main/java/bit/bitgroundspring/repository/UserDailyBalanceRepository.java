// bit/bitgroundspring/repository/UserDailyBalanceRepository.java

package bit.bitgroundspring.repository;

import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.entity.Season;
import bit.bitgroundspring.entity.UserDailyBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;   // LocalDate 임포트
import java.util.List;
import java.util.Optional;

@Repository
public interface UserDailyBalanceRepository extends JpaRepository<UserDailyBalance, Integer> {

    /**
     * 특정 사용자, 시즌, 스냅샷 날짜에 해당하는 일별 잔고 스냅샷을 조회합니다.
     * `uq_user_daily_balances_user_season_snapshot` unique constraint에 의해 하나의 결과만 반환됩니다.
     *
     * @param user 조회할 User 엔티티
     * @param season 조회할 Season 엔티티
     * @param snapshotDate 조회할 스냅샷 날짜 (필드명과 일치)
     * @return 해당 UserDailyBalance 엔티티 (존재할 경우)
     */
    Optional<UserDailyBalance> findByUserAndSeasonAndSnapshotDate(User user, Season season, LocalDate snapshotDate);

    /**
     * 특정 사용자 및 시즌에 대해 지정된 기간 내의 모든 일별 잔고 스냅샷을 조회합니다.
     *
     * @param user 조회할 User 엔티티
     * @param season 조회할 Season 엔티티
     * @param start 조회 시작 날짜
     * @param end 조회 종료 날짜
     * @return 기간 내의 UserDailyBalance 엔티티 목록 (snapshotDate 오름차순 정렬)
     */
    List<UserDailyBalance> findByUserAndSeasonAndSnapshotDateBetweenOrderBySnapshotDateAsc(User user, Season season, LocalDate start, LocalDate end);

    /**
     * 특정 사용자 및 시즌에 대해 가장 최근의 일별 잔고 스냅샷을 조회합니다.
     *
     * @param user 조회할 User 엔티티
     * @param season 조회할 Season 엔티티
     * @return 가장 최근의 UserDailyBalance 엔티티 (존재할 경우)
     */
    Optional<UserDailyBalance> findTopByUserAndSeasonOrderBySnapshotDateDesc(User user, Season season);
}
