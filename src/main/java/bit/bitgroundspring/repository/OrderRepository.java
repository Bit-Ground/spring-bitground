package bit.bitgroundspring.repository;

import bit.bitgroundspring.dto.TradeDto;
import bit.bitgroundspring.dto.projection.OrderProjection;
import bit.bitgroundspring.entity.Order;
import bit.bitgroundspring.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    @Query("SELECT c.symbol AS symbol, c.koreanName AS coinName, " +
            "o.amount AS amount, o.tradePrice AS tradePrice, " +
            "o.createdAt AS createdAt, o.updatedAt AS updatedAt, o.orderType AS orderType " +
            "FROM Order o JOIN o.coin c " +
            "WHERE o.season.id = :seasonId AND o.user.id = :userId")
    List<OrderProjection> findBySeasonIdAndUserId(
            @Param("seasonId") Integer seasonId,
            @Param("userId") Integer userId);

    @Query(
            "SELECT new bit.bitgroundspring.dto.TradeDto( " +
                    "    o.coin.symbol,        " + // String
                    "    o.coin.koreanName,    " + // String
                    "    o.orderType,          " + // OrderType
                    "    o.amount,             " + // Float
                    "    o.tradePrice,         " + // Float
                    "    o.createdAt,          " + // LocalDateTime
                    "    o.updatedAt           " + // LocalDateTime
                    ") " +
                    "FROM Order o " +
                    "WHERE o.coin.symbol = :symbol " +
                    "  AND o.status       = :status " +
                    "ORDER BY o.createdAt DESC"
    )
    List<TradeDto> findTop30ByCoinSymbolAndStatus(
            @Param("symbol") String symbol,
            @Param("status") Status status
    );

    @Query("""
        SELECT new bit.bitgroundspring.dto.TradeDto(
            o.coin.symbol,
            o.coin.koreanName,
            o.orderType,
            o.amount,
            o.tradePrice,
            o.createdAt,
            o.updatedAt
        )
        FROM Order o
        WHERE o.coin.symbol = :symbol
          AND o.status       = :status
          AND o.createdAt > :since
        ORDER BY o.createdAt ASC
    """)
    List<TradeDto> findNewTradesByCoinSymbolAndStatusSince(
            @Param("symbol") String symbol,
            @Param("status") Status status,
            @Param("since") LocalDateTime since
    );
}
