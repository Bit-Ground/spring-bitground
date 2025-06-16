package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.BoardDto;
import bit.bitgroundspring.service.RankService;
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
    private final RankService rankService;

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
            Integer userId = (Integer) row[1]; // 먼저 userId 꺼내고
            int highestTier = rankService.getHighestTierByUserId(userId); // 랭크 서비스 호출

            BoardDto dto = new BoardDto(
                    (Integer) row[0], // postId
                    userId,
                    (String) row[2],  // name
                    (String) row[3],  // profileImage
                    (String) row[4],  // title
                    (String) row[5],  // content
                    ((Number) row[6]).intValue(), // tier
                    ((Number) row[7]).intValue(), // likes
                    ((Number) row[8]).intValue(), // dislikes
                    Boolean.TRUE.equals(row[9]),  // isDeleted
                    ((Timestamp) row[10]).toLocalDateTime(), // createdAt
                    ((Timestamp) row[11]).toLocalDateTime(), // updatedAt
                    (String) row[12],  // category
                    ((Number) row[13]).intValue(), // views
                    ((Number) row[14]).longValue(), // commentCount
                    highestTier   // ✅ 여기 추가
            );

            String content = (String) row[4];
            dto.setHasImage(content != null && content.contains("<img"));

            return dto;
        });
    }

}



