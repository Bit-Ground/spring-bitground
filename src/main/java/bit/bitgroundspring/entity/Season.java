package bit.bitgroundspring.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seasons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "startAt")
    private LocalDateTime startAt = LocalDateTime.now();

    @Column(name = "endAt")
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SeasonStatus status = SeasonStatus.INPROGRESS;

    @Column(name = "rewardCalculated")
    private boolean rewardCalculated;
}
