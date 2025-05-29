package bit.bitgroundspring.service;

import bit.bitgroundspring.repository.UserAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAssetService {
    private final UserAssetRepository userAssetRepository;

    /** userId 로 보유 코인 심볼 리스트만 **/
    public List<String> listOwnedSymbols(Integer userId) {
        return userAssetRepository.findOwnedSymbolsByUserId(userId);
    }
}