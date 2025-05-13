package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_assets")
@IdClass(UserAssetId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAsset {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", referencedColumnName = "userId")
    private User user;

    @Id
    @Column(name = "market")
    private Integer market; // 나중에 Market 테이블 생기면 ManyToOne으로 변경

    @Column(name = "quantity", nullable = false)
    private Float quantity;

    @Column(name = "avgBuyPrice", nullable = false)
    private Float avgBuyPrice;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;
}