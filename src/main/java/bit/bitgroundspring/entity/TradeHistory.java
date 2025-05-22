package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;
import bit.bitgroundspring.entity.TradeType;

@Entity
@Table(name = "trade_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", referencedColumnName = "userId", nullable = false)
    private User user;

    @Column(nullable = false)
    private String market;

    @Column(nullable = false)
    private float price;

    @Column(nullable = false)
    private float quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType type;

    @Column(nullable = false)
    private Timestamp createdAt;
}
