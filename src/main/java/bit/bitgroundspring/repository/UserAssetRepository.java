package bit.bitgroundspring.repository;

import bit.bitgroundspring.dto.projection.UserAssetProjection;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.entity.UserAsset;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserAssetRepository extends JpaRepository<UserAsset, Integer> {
    //유저가 가지고있는 보유자산 목록
    List<UserAsset> findByUser(User user);

    @Query("""
      select ua.coin.symbol
      from UserAsset ua
      where ua.user.id = :userId
        and ua.amount > 0
    """)
    List<String> findOwnedSymbolsByUserId(@Param("userId") Integer userId);
    
    
    /**
     * 유저의 보유 자산 정보 조회
     * @param userId 유저 ID
     * @return 보유 자산 정보 리스트
     */
    @Query("SELECT c.symbol as symbol, ua.amount as amount, ua.avgPrice as avgPrice " +
            "FROM UserAsset ua " +
            "JOIN ua.coin c " +
            "WHERE ua.user.id = :userId")
    List<UserAssetProjection> findUserAssetProjectionsByUserId(@Param("userId") Integer userId);
}
