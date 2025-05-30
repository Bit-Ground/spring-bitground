package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_indices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MarketIndex {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "date", nullable = false)
    private LocalDate date;
    
    @Column(name = "hour", nullable = false, columnDefinition = "tinyint")
    private Integer hour;
    
    @Column(name = "market_index", nullable = false)
    private Integer marketIndex;
    
    @Column(name = "alt_index", nullable = false)
    private Integer altIndex;
    
    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime createdAt;
}
