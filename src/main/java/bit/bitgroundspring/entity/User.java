package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userId")
    private Integer id;

    @Column(nullable = false)
    private String provider; // oauth2 제공자 (google, naver, kakao)

    @Column(nullable = false)
    private String providerId; // oauth2 제공자의 subject id

    @Column(nullable = false)
    private String name;

    @Column
    private String email;

    @Column
    private String profileImage;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}