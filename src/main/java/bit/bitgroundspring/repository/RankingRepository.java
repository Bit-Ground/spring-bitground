package bit.bitgroundspring.repository;

import bit.bitgroundspring.entity.Status;
import bit.bitgroundspring.entity.UserRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RankingRepository extends JpaRepository<UserRanking, Integer> {

    /**
     * ✅ 실시간 랭킹 조회용 메서드
     * - 특정 시즌 ID에 해당하는 유저 랭킹 목록을 조회
     * - `UserRanking`과 연관된 `User` 정보를 함께 즉시 로딩 (fetch join)
     * - 총 자산(totalValue) 기준으로 내림차순 정렬
     *
     * ⚠️ Lazy 로딩으로 인해 user.getProfileImage() 등을 사용할 때
     *    N+1 문제가 생기지 않도록 fetch join을 사용
     *
     * @param seasonId 시즌 ID (ex. 현재 시즌 ID)
     * @return 시즌에 참여한 유저들의 실시간 랭킹 리스트
     */
    @Query("SELECT ur FROM UserRanking ur JOIN FETCH ur.user WHERE ur.season.id = :seasonId ORDER BY ur.totalValue DESC")
    List<UserRanking> findWithUserBySeasonId(@Param("seasonId") int seasonId);

    /**
     * ✅ 전 시즌 랭킹 조회용 메서드
     * - 완료된 시즌(COMPLETED) 상태의 랭킹 목록을 조회
     * - `UserRanking`과 연관된 `User`, `Season` 정보를 함께 fetch join
     * - 시즌 ID 기준으로 최신 시즌부터 정렬
     * - 동일 시즌 내에서는 totalValue(총 자산) 기준으로 내림차순 정렬
     *
     * ex) 시즌별로 상위 유저 랭킹을 전시할 때 사용
     *
     * @param status 시즌 상태값 (Status.COMPLETED)
     * @return 전 시즌 랭킹 데이터 리스트
     */
    @Query("SELECT ur FROM UserRanking ur JOIN FETCH ur.user JOIN FETCH ur.season s WHERE s.status = :status ORDER BY s.id DESC, ur.totalValue DESC")
    List<UserRanking> findBySeason_StatusOrderBySeason_IdDescTotalValueDesc(@Param("status") Status status);
}
