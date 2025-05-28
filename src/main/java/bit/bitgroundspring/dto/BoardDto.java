package bit.bitgroundspring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardDto {
    private Integer id;
    private Integer userId;      // User 엔티티 대신 ID만
    private String name;         // 작성자 이름 (응답용)
    private String title;
    private String content;
    private int tier;
    private int likes;
    private int dislikes;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String category;     // Enum.name() 형식으로 전달
    private int views;
}