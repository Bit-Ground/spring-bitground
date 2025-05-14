package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 공지사항 ID (자동 증가)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User userId; // 공지 작성자 (외래키)

    @Column(name = "title", nullable = false, length = 255)
    private String title; // 공지 제목

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content; // 공지 내용

    @Column(name = "createdAt", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt; // 작성일 (기본값: 현재 시간)

    // 기본 생성자
    public Notice() {}

    // 생성자 (필요 시 추가)
    public Notice(User userId, String title, String content) {
        this.userId = userId;
        this.title = title;
        this.content = content;
    }
}
