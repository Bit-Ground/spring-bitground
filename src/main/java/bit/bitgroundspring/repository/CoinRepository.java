package bit.bitgroundspring.repository;

import bit.bitgroundspring.entity.Coin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoinRepository extends JpaRepository<Coin, Integer> {
    // symbol 필드를 기준으로 코인을 조회하기 위한 사용자 정의 메서드
    Optional<Coin> findBySymbol(String symbol);
}
