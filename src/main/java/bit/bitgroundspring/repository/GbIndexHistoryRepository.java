package bit.bitgroundspring.repository;

import bit.bitgroundspring.entity.MarketIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GbIndexHistoryRepository extends JpaRepository<MarketIndex, Long> {
    //가장 최근 값 조회
}
