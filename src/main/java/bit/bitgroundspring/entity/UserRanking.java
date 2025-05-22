package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_rankings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", referencedColumnName = "userId")
    private User userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seasonId", referencedColumnName = "id", nullable = false)
    private Season season;

    @Column(name = "ranks", nullable = false)
    private Integer ranks;

    @Column(name = "totalValue", nullable = false)
    private Float totalValue;
}
