package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.BoardDto;
import bit.bitgroundspring.entity.Post;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.repository.UserRepository;
import org.springframework.stereotype.Service;

import bit.bitgroundspring.repository.BoardRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    public Post savePost(BoardDto dto) {
        // 1. 사용자 ID로 User 객체 조회
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. Post 엔터티 생성
        Post post = new Post();
        post.setUser(user);
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setFilePath(dto.getFilePath());
        post.setFileName(dto.getFileName());
        post.setCategory(dto.getCategory());

        // 3. 저장
        return boardRepository.save(post);
    }
}
