package bit.bitgroundspring.repository;

import bit.bitgroundspring.entity.AiAdvice;
import bit.bitgroundspring.entity.Season;
import bit.bitgroundspring.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiAdviceRepository extends JpaRepository<AiAdvice, Integer> {

    /**
     * 특정 사용자와 시즌에 대한 AI 조언을 조회합니다.
     * AiAdvice 엔티티에 user_id와 season_id 조합으로 Unique Constraint가 있으므로,
     * 해당 조합으로 하나의 결과만 반환되어야 합니다.
     *
     * @param user 조회할 User 엔티티
     * @param season 조회할 Season 엔티티
     * @return 해당 AiAdvice 엔티티 (존재할 경우)
     */
    Optional<AiAdvice> findByUserAndSeason(User user, Season season);

    /**
     * 사용자 ID와 시즌 ID를 사용하여 AI 조언을 조회합니다.
     * API 엔드포인트에서 ID를 받아 직접 조회할 때 유용합니다.
     *
     * @param userId 조회할 사용자의 ID
     * @param seasonId 조회할 시즌의 ID
     * @return 해당 AiAdvice 엔티티 (존재할 경우)
     */
    Optional<AiAdvice> findByUser_IdAndSeason_Id(Integer userId, Integer seasonId);
}
