package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.BoardDto;
import bit.bitgroundspring.entity.Category;
import bit.bitgroundspring.entity.Post;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import bit.bitgroundspring.repository.BoardRepository;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    //게시글 저장
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
                .views(dto.getViews())
                .likes(0)
                .dislikes(0)
                .isDeleted(false)
                .build();

        // 3. 저장
        return boardRepository.save(post);
    }

    public Page<BoardDto> getBoardDtos(String category, Pageable pageable) {
        Page<Object[]> rowsPage = boardRepository.findAllBoardDtosRaw(category, pageable);

        return rowsPage.map(row -> {
            BoardDto dto = new BoardDto(
                    (Integer) row[0], // p.id
                    (Integer) row[1], // userId
                    (String) row[2],  // u.name
                    (String) row[3],  // ✅ u.profile_image
                    (String) row[4],  // title
                    (String) row[5],  // content
                    ((Number) row[6]).intValue(), // tier
                    ((Number) row[7]).intValue(), // likes
                    ((Number) row[8]).intValue(), // dislikes
                    Boolean.TRUE.equals(row[9]),  // is_deleted
                    ((Timestamp) row[10]).toLocalDateTime(), // created_at
                    ((Timestamp) row[11]).toLocalDateTime(), // updated_at
                    (String) row[12],  // category
                    ((Number) row[13]).intValue(), // views
                    ((Number) row[14]).longValue() // commentCount
            );

            // ✅ <img 태그 포함 여부 확인해서 세팅
            String content = (String) row[4];
            dto.setHasImage(content != null && content.contains("<img"));

            return dto;
        });
    }

}



