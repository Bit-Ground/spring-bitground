package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAsset {

    @EmbeddedId
    private UserAssetId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", referencedColumnName = "userId")
    private User user;

    @Column(name = "quantity", nullable = false)
    private Float quantity;

    @Column(name = "avgBuyPrice", nullable = false)
    private Float avgBuyPrice;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;
}