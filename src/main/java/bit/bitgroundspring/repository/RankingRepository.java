package bit.bitgroundspring.repository;

import bit.bitgroundspring.entity.UserRanking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RankingRepository extends JpaRepository<UserRanking, Integer > {
    // 시즌 ID로 해당 시즌의 유저 랭킹 리스트 조회 (총 자산 내림차순 정렬)
    List<UserRanking> findBySeason_IdOrderByTotalValueDesc(int seasonId);

    // 특정 시즌 기준으로 랭킹 전체를 총자산 내림차순으로 정렬해서 조회
//    List<UserRanking> findBySeasonOrderByTotalValueDesc(Season season);

}
