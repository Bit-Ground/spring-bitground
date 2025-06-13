package bit.bitgroundspring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NoticeResponseDto {
    private Integer id;
    private String title;
    private String writer; // user.name
    private String content;
    private LocalDateTime createdAt;
}