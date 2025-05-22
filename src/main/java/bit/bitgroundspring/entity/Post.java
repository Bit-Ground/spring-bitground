package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 게시글 ID (자동 증가)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User userId; // 작성한 유저 (외래키)

    @Column(name = "title", nullable = false, length = 255, columnDefinition = "VARCHAR(255) DEFAULT ''")
    private String title; // 게시글 제목

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content; // 게시글 내용

    @Column(name = "filePath", columnDefinition = "VARCHAR(255) DEFAULT NULL")
    private String filePath; // 첨부파일 경로

    @Column(name = "fileName", columnDefinition = "VARCHAR(255) DEFAULT NULL")
    private String fileName; // 첨부파일 원본 파일명

    @Column(name = "likes", columnDefinition = "INT DEFAULT 0")
    private int likes; // 게시글 추천수

    @Column(name = "reports", columnDefinition = "INT DEFAULT 0")
    private int reports; // 게시글 신고수

    @Column(name = "createdAt", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt; // 작성일 (기본값: 현재 시간)

    @Column(name = "updatedAt")
    private LocalDateTime updatedAt; // 수정일 (수정 시 자동 업데이트)

    @Column(name = "deletedAt")
    private LocalDateTime deletedAt; // 삭제일 (삭제 시 기록)

    @Column(name = "isDeleted", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isDeleted; // 삭제 여부 (논리적 삭제 처리)

    // 기본 생성자
    public Post() {}

    // 생성자 (필요시 추가)
    public Post(User userId, String title, String content, String filePath, String fileName) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.filePath = filePath;
        this.fileName = fileName;
    }
}
