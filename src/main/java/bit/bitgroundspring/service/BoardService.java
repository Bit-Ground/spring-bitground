package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.BoardDto;
import bit.bitgroundspring.entity.Category;
import bit.bitgroundspring.entity.Post;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.repository.UserRepository;
import org.springframework.stereotype.Service;

import bit.bitgroundspring.repository.BoardRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    public Post savePost(BoardDto dto) {
        // 1. 사용자 ID로 User 객체 조회
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. Post 엔터티 생성 및 필드 설정
        Post post = Post.builder()
                .user(user)
                .tier(dto.getTier()) // ✅ 사용자의 tier 설정
                .category(Category.valueOf(dto.getCategory())) // ✅ 문자열 → enum
                .title(dto.getTitle())
                .content(dto.getContent())
                .likes(0)
                .dislikes(0)
                .isDeleted(false)
                .build();

        // 3. 저장
        return boardRepository.save(post);
    }

    public List<BoardDto> getAllPosts() {
        return boardRepository.findAllBoardDtos();
    }
}



