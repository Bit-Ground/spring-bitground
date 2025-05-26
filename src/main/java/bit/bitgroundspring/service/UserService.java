package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.UserUpdate;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    // 소셜 로그인 ID로 사용자 조회
    public Optional<User> getUserBySocialId(String provider, String providerId) {
        return userRepository.findByProviderAndProviderId(provider, providerId);
    }

    // 유저 업데이트
    public User updateUser(Integer userId, UserUpdate request) {
        User user = userRepository.findById(userId).orElseThrow(()-> new RuntimeException("해당 사용자 없음"));

        user.setName(request.getName());
        user.setProfileImage(request.getProfileImage());
        return userRepository.save(user);
    }
}