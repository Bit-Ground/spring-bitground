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

public interface RankRepository extends JpaRepository<UserRanking, Integer> {
    
    // 특정 시즌의 랭킹을 조회하기 위한 사용자 정의 메서드
    @Query("SELECT ur.user.id as userId, " +
            "u.name as name, " +
            "u.profileImage as profileImage, " +
            "ur.ranks as ranks, " +
            "ur.tier as tier, " +
            "ur.totalValue as totalValue " +
            "FROM UserRanking ur " +
            "JOIN ur.user u " +
            "WHERE ur.season.id = :seasonId " +
            "ORDER BY ur.ranks ASC")
    List<RankProjection> findRankingsBySeasonId(@Param("seasonId") Integer seasonId);
    
    // 현재 시즌의 랭킹을 조회하기 위한 사용자 정의 메서드
    @Query("SELECT ur.user.id as userId, " +
            "u.name as name, " +
            "u.profileImage as profileImage, " +
            "ur.ranks as ranks, " +
            "ur.tier as tier, " +
            "ur.totalValue as totalValue, " +
            "ur.updatedAt as updatedAt, " +
            "ur.season.id as seasonId " + //  이 줄 추가
            "FROM UserRanking ur " +
            "JOIN ur.user u " +
            "WHERE ur.season.status = 'PENDING' " +
            "ORDER BY ur.ranks ASC")
    List<RankProjection> findCurrentSeasonRankings();

    Optional<UserRanking> findByUserAndSeason(User user, Season season);

    //툴팁용
    List<UserRanking> findTop5ByUserOrderBySeasonIdDesc(User user);

    @Query("SELECT MAX(ur.tier) FROM UserRanking ur WHERE ur.user = :user")
    Integer findHighestTierByUser(@Param("user") User user);

    @Query("SELECT ur FROM UserRanking ur JOIN FETCH ur.season WHERE ur.user = :user ORDER BY ur.season.id DESC")
    List<UserRanking> findTop5ByUserWithSeason(@Param("user") User user);
}
