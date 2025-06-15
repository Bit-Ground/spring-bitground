package bit.bitgroundspring.repository;

import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.entity.UserRanking;
import bit.bitgroundspring.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRankingRepository extends JpaRepository<UserRanking, Long> {
    Optional<UserRanking> findByUserAndSeason(User user, Season season);
}