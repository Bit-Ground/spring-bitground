package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pending_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PendingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // 외래키: users.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Column(name = "market", nullable = false)
    private int market;  // 코인 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType type;  // 거래 타입 (TradeType enum 사용)

    @Column(nullable = false)
    private float quantity;  // 거래 수량

    @Column(name = "targetPrice", nullable = false)
    private float targetPrice;  // 예약 체결 가격

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'PENDING'")
    private OrderStatus status = OrderStatus.PENDING;  // 거래 상태 (OrderStatus enum 사용)

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();  // 거래 요청 시각

    @Column(name = "triggeredAt")
    private LocalDateTime triggeredAt;  // 거래 체결 시각

    // 생성 시 자동으로 createdAt 설정
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
