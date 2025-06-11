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
    private Long commentCount;
    private boolean hasImage;

    public BoardDto(Integer id, Integer userId, String name, String title, String content,
                    int tier, int likes, int dislikes, boolean isDeleted,
                    LocalDateTime createdAt, LocalDateTime updatedAt, String category,
                    int views, Long commentCount) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.title = title;
        this.content = content;
        this.tier = tier;
        this.likes = likes;
        this.dislikes = dislikes;
        this.isDeleted = isDeleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.category = category;
        this.views = views;
        this.commentCount = commentCount;
        this.hasImage = false; // 기본값 (나중에 setHasImage로 따로 설정 가능)
    }
}

