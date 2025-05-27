package bit.bitgroundspring.controller;

import bit.bitgroundspring.entity.Post;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.repository.BoardRepository;
import bit.bitgroundspring.repository.UserRepository;
import bit.bitgroundspring.naver.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class BoardController {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final ObjectStorageService objectStorageService;

    @Value("${ncp.bucket}")
    private String bucketName;

    // ✅ Quill 에디터용 이미지 업로드
    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String directory = "quill"; // 저장 경로 (선택)
            String imageUrl = objectStorageService.uploadFile(bucketName, directory, file);

            Map<String, String> result = new HashMap<>();
            result.put("url", imageUrl);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("이미지 업로드 실패");
        }
    }
    
}