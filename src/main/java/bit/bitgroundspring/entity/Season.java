package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import bit.bitgroundspring.entity.SeasonStatus;

@Entity
@Table(name = "seasons")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private SeasonStatus status = SeasonStatus.INPROGRESS;

    private Boolean rewardCalculated;

    @PrePersist
    public void prePersist() {
        this.startAt = LocalDateTime.now();
    }
}