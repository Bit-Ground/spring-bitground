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
    private String name;     // 응답용: 작성자 이름
    private String title;
    private String content;
    private String filePath;
    private String fileName;
    private int likes;
    private int reports;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private boolean isDeleted;
    private String category;
}
