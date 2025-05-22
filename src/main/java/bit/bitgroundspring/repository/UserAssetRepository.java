package bit.bitgroundspring.repository;

import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.entity.UserAsset;
import bit.bitgroundspring.entity.UserAssetId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAssetRepository extends JpaRepository<UserAsset, UserAssetId> {
    //유저가 가지고있는 보유자산 목록
    List<UserAsset> findByUser(User user);
}
