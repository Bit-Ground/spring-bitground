package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "email", length = 255)
    private String email;
    
    @Column(name = "name", length = 255, nullable = false)
    private String name;
    
    @Column(name = "profile_image", length = 255)
    private String profileImage;
    
    @Column(name = "provider", length = 255, nullable = false)
    private String provider;
    
    @Column(name = "provider_id", length = 255)
    private String providerId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, columnDefinition = "enum ('ROLE_ADMIN', 'ROLE_USER') default 'ROLE_USER'")
    @Builder.Default
    private Role role = Role.ROLE_USER;
    
    @Column(name = "cash", nullable = false, columnDefinition = "int")
    private Integer cash;
    
    @Column(name = "tier", nullable = false, columnDefinition = "tinyint default 0")
    @Builder.Default
    private Integer tier = 0;
    
    @Column(name = "is_deleted", nullable = false, columnDefinition = "tinyint(1) default 0")
    @Builder.Default
    private Boolean isDeleted = false;
    
    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)")
    private LocalDateTime updatedAt;
}