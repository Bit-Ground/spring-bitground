package bit.bitgroundspring.repository;

import bit.bitgroundspring.dto.projection.RankProjection;
import bit.bitgroundspring.entity.UserRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
            "ur.updatedAt as updatedAt " +
            "FROM UserRanking ur " +
            "JOIN ur.user u " +
            "WHERE ur.season.status = 'PENDING' " +
            "ORDER BY ur.ranks ASC")
    List<RankProjection> findCurrentSeasonRankings();
}
