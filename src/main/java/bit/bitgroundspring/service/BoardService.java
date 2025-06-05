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

    //게시글 목록 출력
//    public List<BoardDto> getAllBoardDtos() {
//        List<Object[]> rows = boardRepository.findAllBoardDtosRaw();
//
//        List<BoardDto> result = new ArrayList<>();
//
//        for (Object[] row : rows) {
//            BoardDto dto = new BoardDto(
//                    (Integer) row[0], // p.id
//                    (Integer) row[1], // userId
//                    (String) row[2],  // u.name
//                    (String) row[3],  // title
//                    (String) row[4],  // content
//                    ((Number) row[5]).intValue(), // tier
//                    ((Number) row[6]).intValue(), // likes
//                    ((Number) row[7]).intValue(), // dislikes
//                    Boolean.TRUE.equals(row[8]), // ✅ isDeleted
//                    ((Timestamp) row[9]).toLocalDateTime(), // ✅ createdAt
//                    ((Timestamp) row[10]).toLocalDateTime(), // ✅ updatedAt
//                    (String) row[11],  // ✅ category
//                    ((Number) row[12]).intValue()// ✅ views
//            );
//            result.add(dto);
//        }
//        return result;
//    }
    public Page<BoardDto> getBoardDtos(String category, Pageable pageable) {
        Page<Object[]> rowsPage = boardRepository.findAllBoardDtosRaw(category, pageable);

        return rowsPage.map(row -> new BoardDto(
                (Integer) row[0], // p.id
                (Integer) row[1], // userId
                (String) row[2],  // u.name
                (String) row[3],  // title
                (String) row[4],  // content
                ((Number) row[5]).intValue(), // tier
                ((Number) row[6]).intValue(), // likes
                ((Number) row[7]).intValue(), // dislikes
                Boolean.TRUE.equals(row[8]),  // is_deleted
                ((Timestamp) row[9]).toLocalDateTime(), // created_at
                ((Timestamp) row[10]).toLocalDateTime(), // updated_at
                (String) row[11],  // category
                ((Number) row[12]).intValue(), // views
                ((Number) row[13]).longValue()
        ));
    }

}



