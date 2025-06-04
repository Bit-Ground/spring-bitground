package bit.bitgroundspring.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CommentResponseDto {
    private Integer id;
    private Integer userId;
    private Integer parentId;
    private String content;
    private String userName;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private List<CommentResponseDto> children;
}
