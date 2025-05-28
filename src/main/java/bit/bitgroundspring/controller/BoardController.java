package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.BoardDto;
import bit.bitgroundspring.entity.Post;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.naver.NcpObjectStorageService;
import bit.bitgroundspring.repository.BoardRepository;
import bit.bitgroundspring.repository.RankingRepository;
import bit.bitgroundspring.repository.UserRepository;
import bit.bitgroundspring.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class BoardController {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final NcpObjectStorageService objectStorageService;
    private final BoardService boardService;
    private final RankingRepository rankingRepository;

    @Value("${ncp.bucket}")
    private String bucketName;

    // ✅ Quill 에디터용 이미지 업로드
    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String directory = "posts"; // 저장 경로 (선택)
            String fileName = objectStorageService.uploadFile(bucketName, directory, file);
            String fileUrl = "https://bitground.kr.object.ncloudstorage.com/posts/" + fileName;

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("이미지 업로드 실패");
        }
    }

    // ✅ 게시글 등록 (FormData 기반)
    @PostMapping("/form")
    public ResponseEntity<?> createPost(@RequestBody Post post) {
        System.out.println(post.getUser().getId());
        System.out.println(post.getUser().getTier());
        try {
            if (post.getUser() == null || post.getUser().getId() == null) {
                return ResponseEntity.badRequest().body("userId 누락");
            }

            // DB에서 영속 객체로 교체
            User user = userRepository.findById(post.getUser().getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            post.setUser(user);
            post.setTier(user.getTier());

            boardRepository.save(post);
            return ResponseEntity.ok("글 등록 성공");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("글 등록 실패");
        }
    }

    // 게시글 목록 출력
    @GetMapping("/list")
    public ResponseEntity<?> getPostList() {
        List<BoardDto> posts = boardService.getAllBoardDtos();
        return ResponseEntity.ok(posts);
    }

    // 게시글 상세보기
    @GetMapping("/{id}")
    public ResponseEntity<?> getPostDetail(@PathVariable Integer id) {
        Post post = boardRepository.findWithUserById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글이 없습니다."));

        boolean hasImage = post.getContent() != null && post.getContent().contains("<img");

        BoardDto dto = new BoardDto(
                post.getId(),
                post.getUser().getId(),
                post.getUser().getName(),
                post.getTitle(),
                post.getContent(),
                post.getTier(),
                post.getLikes(),
                post.getDislikes(),
                post.getIsDeleted(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getCategory().name(),
                post.getViews()
        );

        return ResponseEntity.ok(dto);
    }
}

