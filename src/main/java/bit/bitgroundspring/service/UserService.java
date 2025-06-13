package bit.bitgroundspring.service;

import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.naver.NcpObjectStorageService;
import bit.bitgroundspring.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String providerId = authentication.getName();
        String provider = (String) authentication.getDetails();

        return getUserBySocialId(provider, providerId)
                .orElseThrow(() -> new RuntimeException("해당 유저를 찾을 수 없습니다."));
    }
    
    private final UserRepository userRepository;
    private final NcpObjectStorageService objectStorageService;

    @Value("${ncp.bucket}")
    private String bucketName;
    
    // 소셜 로그인 ID로 사용자 조회
    public Optional<User> getUserBySocialId(String provider, String providerId) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .filter(user -> !user.getIsDeleted());
    }

    // 유저 업데이트
    @Transactional
    public User updateUser(Integer userId, String name, String email, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("해당 사용자 없음"));

        user.setName(name);
        user.setEmail(email);

        if (imageUrl != null && !imageUrl.equals(user.getProfileImage())) {
            // 이전 이미지 삭제
            String oldImage = user.getProfileImage();
            if (oldImage != null && !oldImage.isEmpty()) {
                // 파일명 추출 (예: https://kr.object.ncloudstorage.com/bucket/profile/abc.png → abc.png)
                String oldFileName = oldImage.substring(oldImage.lastIndexOf("/") + 1);
                objectStorageService.deleteFile(bucketName, "profile", oldFileName);
            }

            // 새 이미지 저장
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

    //isDeleted -> 1
    public void softDeleteUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("해당 사용자 없음"));
        user.setIsDeleted(true);
        user.setTier(0);
        user.setEmail(null);
        user.setProviderId(null);
        userRepository.save(user);
    }

}