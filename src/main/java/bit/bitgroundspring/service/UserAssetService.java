package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.projection.UserAssetProjection;
import bit.bitgroundspring.dto.response.UserAssetResponse;
import bit.bitgroundspring.entity.Coin;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.entity.UserAsset;
import bit.bitgroundspring.repository.CoinRepository;
import bit.bitgroundspring.repository.UserAssetRepository;
import bit.bitgroundspring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserAssetService {
    private  final UserRepository userRepository;
    private final UserAssetRepository userAssetRepository;
    private final CoinRepository coinRepository;

    /** userId 로 보유 코인 심볼 리스트만 **/
    public List<String> listOwnedSymbols(Integer userId) {
        return userAssetRepository.findOwnedSymbolsByUserId(userId);
    }

    @Transactional
    public Optional<UserAsset> findByUserAndCoin(Integer userId, String symbol) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Coin coin = coinRepository.findBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("Coin not found"));
        return userAssetRepository.findByUserAndCoinWithLock(user, coin);
    }
    
    /**
     * userId 로 보유 자산들 조회
     * @param userId 유저 ID
     * @return 보유 자산 정보 리스트
     */
    @Transactional(readOnly = true)
    public UserAssetResponse getUserAssets(Integer userId) {
        // 사용자 현금 조회
        int cash = userRepository.findById(userId)
                .map(User::getCash)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 사용자 자산 조회 (projection 사용)
        List<UserAssetProjection> userAssets = userAssetRepository
                .findUserAssetProjectionsByUserId(userId);
        
        return new UserAssetResponse(cash, userAssets);
    }
}