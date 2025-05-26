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

    // ✅ 게시글 등록 (FormData 기반)
    @PostMapping("/form")
    public ResponseEntity<?> createPost(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("category") String category,
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Post post = new Post();
            post.setUser(user);
            post.setTitle(title);
            post.setContent(content);
            post.setCategory(category);

            if (file != null && !file.isEmpty()) {
                String directory = "posts"; // 게시글 업로드 경로
                String fileUrl = objectStorageService.uploadFile(bucketName, directory, file);
                post.setFilePath(fileUrl);
                post.setFileName(file.getOriginalFilename());
            }

            boardRepository.save(post);
            return ResponseEntity.ok("글 등록 성공");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("글 등록 실패");
        }
    }
}