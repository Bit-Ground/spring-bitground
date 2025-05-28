package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "coins")
@Getter
@Setter
@NoArgsConstructor // @NoArgsConstructor(access = AccessLevel.PROTECTED) 대신 (Coin::new 오류 해결)
@AllArgsConstructor
@Builder
public class Coin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "symbol", length = 255, nullable = false, unique = true)
    private String symbol;

    @Column(name = "korean_name", length = 255, nullable = false)
    private String koreanName;

    @Column(name = "trade_price_24h", nullable = false)
    private Long tradePrice24h;

    @Column(name = "change_rate")
    private Float changeRate;

    @Column(name = "is_caution", nullable = false, columnDefinition = "tinyint(1) default 0")
    @Builder.Default
    private Boolean isCaution = false;

    @Column(name = "is_warning", nullable = false, columnDefinition = "tinyint(1) default 0")
    @Builder.Default
    private Boolean isWarning = false;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "tinyint(1) default 0")
    @Builder.Default
    private Boolean isDeleted = false;
    
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)")
    private LocalDateTime updatedAt;
}