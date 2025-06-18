package bit.bitgroundspring.repository;

import bit.bitgroundspring.dto.projection.RankProjection;
import bit.bitgroundspring.entity.Season;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.entity.UserRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RankRepository extends JpaRepository<UserRanking, Long> {

    // ✅ 단일 사용자 + 시즌별 랭킹 조회
    Optional<UserRanking> findByUserAndSeason(User user, Season season);

    // ✅ 특정 시즌의 전체 랭킹 목록 (Projection)
    @Query("SELECT ur.user.id as userId, " +
            "u.name as name, " +
            "u.profileImage as profileImage, " +
            "ur.ranks as ranks, " +
            "ur.tier as tier, " +
            "ur.totalValue as totalValue, " +
            "ur.updatedAt as updatedAt, " +
            "ur.season.id as seasonId, " +
            "ur.season.name as seasonName " +
            "FROM UserRanking ur " +
            "JOIN ur.user u " +
            "WHERE ur.season.id = :seasonId " +
            "ORDER BY ur.ranks ASC")
    List<RankProjection> findRankingsBySeasonId(@Param("seasonId") Integer seasonId);

    // ✅ 현재 시즌 랭킹 목록
    @Query("SELECT ur.user.id as userId, " +
            "u.name as name, " +
            "u.profileImage as profileImage, " +
            "ur.ranks as ranks, " +
            "ur.tier as tier, " +
            "ur.totalValue as totalValue, " +
            "ur.updatedAt as updatedAt, " +
            "ur.season.id as seasonId, " +
            "ur.season.name as seasonName " +
            "FROM UserRanking ur " +
            "JOIN ur.user u " +
            "WHERE ur.season.status = 'PENDING' " +
            "ORDER BY ur.ranks ASC")
    List<RankProjection> findCurrentSeasonRankings();

    // ✅ 툴팁용: 최근 시즌 랭킹 최대 5개 (시즌 ID 순 정렬)
    List<UserRanking> findTop5ByUserOrderBySeasonIdDesc(User user);

    // ✅ 툴팁용: 시즌 엔티티 포함해서 최대 5개
    @Query("SELECT ur FROM UserRanking ur JOIN FETCH ur.season WHERE ur.user = :user ORDER BY ur.season.id DESC")
    List<UserRanking> findTop5ByUserWithSeason(@Param("user") User user);

    // ✅ 현재 시즌 제외하고 최고 티어 찾기
    @Query("SELECT MAX(r.tier) FROM UserRanking r WHERE r.user = :user AND r.season.name <> :seasonName")
    Integer findHighestTierByUserExcludingSeason(@Param("user") User user, @Param("seasonName") String seasonName);

    // ✅ 전체 시즌 중 최고 티어
    @Query("SELECT MAX(r.tier) FROM UserRanking r WHERE r.user = :user")
    Integer findHighestTierByUser(@Param("user") User user);
}