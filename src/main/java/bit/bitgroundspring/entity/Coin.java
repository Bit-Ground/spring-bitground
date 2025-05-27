package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "coins")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Coin {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "symbol", length = 255, nullable = false)
    private String symbol;
    
    @Column(name = "trade_volume")
    private Float tradeVolume;
    
    @Column(name = "change_rate")
    private Float changeRate;
    
    @Column(name = "is_warning", nullable = false, columnDefinition = "tinyint(1) default 0")
    @Builder.Default
    private Boolean isWarning = false;
    
    @Column(name = "is_caution", nullable = false, columnDefinition = "tinyint(1) default 0")
    @Builder.Default
    private Boolean isCaution = false;
    
    @Column(name = "is_deleted", nullable = false, columnDefinition = "tinyint(1) default 0")
    @Builder.Default
    private Boolean isDeleted = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(6)")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime(6)")
    private LocalDateTime updatedAt;
}