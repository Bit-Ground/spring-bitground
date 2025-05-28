package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal; // BigDecimal 임포트 확인!
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

    // --- 수정된 부분: 거래대금 (trade_price_24h) 필드 추가/변경 ---
    @Column(name = "trade_price_24h", nullable = false) //
    private Long tradePrice24h; // 24시간 누적 거래대금

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime(6)")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime(6)")
    private LocalDateTime updatedAt;
}