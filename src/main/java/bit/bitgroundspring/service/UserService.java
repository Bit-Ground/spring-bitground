package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.UserUpdate;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.naver.NcpObjectStorageService;
import bit.bitgroundspring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final NcpObjectStorageService objectStorageService;

    @Value("${ncp.bucket}")
    private String bucketName;
    
    // 소셜 로그인 ID로 사용자 조회
    public Optional<User> getUserBySocialId(String provider, String providerId) {
        return userRepository.findByProviderAndProviderId(provider, providerId);
    }

    // 유저 업데이트
    public User updateUser(Integer userId, String name, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("해당 사용자 없음"));

        user.setName(name);
        if (imageUrl != null) {
            user.setProfileImage(imageUrl);
        }

        return userRepository.save(user);
    }

    //이미지업로드
    public String uploadProfileImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 선택되지 않았습니다.");
        }
        try {
            String uploadedFilename = objectStorageService.uploadFile(bucketName, "profile", file);

            return "https://kr.object.ncloudstorage.com/" + bucketName + "/profile/" + uploadedFilename;
        } catch (Exception e) {
            throw new RuntimeException("파일 업로드 실패", e);
        }
    }
}