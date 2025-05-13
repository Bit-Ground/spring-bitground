package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "inquiries")
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 문의 ID (자동 증가)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user; // 문의 작성자 (외래키)

    @Column(name = "title", nullable = false, length = 255)
    private String title; // 문의 제목

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content; // 문의 내용

    @Column(name = "filePath", length = 255)
    private String filePath; // 파일 경로

    @Column(name = "fileName", length = 255)
    private String fileName; // 원본 파일명

    @Column(name = "extension", length = 10)
    private String extension; // 파일 확장자

    @Column(name = "createdAt", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt; // 작성일 (기본값: 현재 시간)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status; // 문의 상태 (PENDING, etc.)

    // 기본 생성자
    public Inquiry() {}

    // 생성자 (필요 시 추가)
    public Inquiry(User user, String title, String content, String filePath, String fileName, String extension, OrderStatus status) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.filePath = filePath;
        this.fileName = fileName;
        this.extension = extension;
        this.status = status;
    }
}
