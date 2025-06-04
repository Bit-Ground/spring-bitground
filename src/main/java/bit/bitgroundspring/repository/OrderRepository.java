package bit.bitgroundspring.repository;

import bit.bitgroundspring.dto.projection.OrderProjection;
import bit.bitgroundspring.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    @Query("SELECT c.symbol AS symbol, c.koreanName AS coinName, " +
            "o.amount AS amount, o.tradePrice AS tradePrice, " +
            "o.createdAt AS createdAt, o.updatedAt AS updatedAt, o.orderType AS orderType " +
            "FROM Order o JOIN o.coin c " +
            "WHERE o.season.id = :seasonId AND o.user.id = :userId")
    List<OrderProjection> findBySeasonIdAndUserId(
            @Param("seasonId") Long seasonId,
            @Param("userId") Integer userId);
}
