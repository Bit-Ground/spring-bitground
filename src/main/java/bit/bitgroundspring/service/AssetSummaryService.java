package bit.bitgroundspring.service;

import bit.bitgroundspring.repository.UserAssetRepository;
import bit.bitgroundspring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetSummaryService {
    private final UserAssetRepository userAssetRepository;
    private final UserRepository userRepository;
}
