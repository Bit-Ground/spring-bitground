package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 댓글 ID (자동 증가)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postId", nullable = false)
    private Post post; // 해당 댓글이 속한 게시글 (외래키)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user; // 댓글 작성 유저 (외래키)

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content; // 댓글 내용

    @Column(name = "createdAt", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt; // 댓글 작성일 (기본값: 현재 시간)

    @Column(name = "likes", columnDefinition = "INT DEFAULT 0")
    private int likes; // 댓글 추천수 (기본값: 0)

    // 기본 생성자
    public Comment() {}

    // 생성자 (필요 시 추가)
    public Comment(Post post, User user, String content) {
        this.post = post;
        this.user = user;
        this.content = content;
    }
}
