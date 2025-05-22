package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "users_seasons_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSeasonHistory {

    @Id
    @Column(nullable = false)
    private Long userId;

    @Id
    @Column(nullable = false)
    private Long seasonId;

    @Column(nullable = false)
    private String tier;

    @Column(nullable = false)
    private int ranks;

    @Column(nullable = false)
    private float profitRate;

}
