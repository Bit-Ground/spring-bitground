package bit.bitgroundspring.repository;

import bit.bitgroundspring.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeasonRepository extends JpaRepository<Season, Long> {
    // 종료되지 않은 시즌 하나 가져오기 (진행 중인 시즌)
    Season findFirstByEndAtIsNull();
}
