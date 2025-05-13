package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_history")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // ✅ 외래키: users.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Column(name = "market", nullable = false)
    private Coin coin;

    @Column(nullable = false)
    private float price;

    @Column(nullable = false)
    private float quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType type;

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();  // 기본값

    // 생성자나 @PrePersist 사용 가능
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}