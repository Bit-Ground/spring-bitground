package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "favorite_coins")
public class FavoriteCoin {

    @EmbeddedId
    private FavoriteCoinId id;  // 복합키 사용

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")  // 복합키의 userId 필드를 사용하여 user 엔티티를 매핑
    @JoinColumn(name = "userId", nullable = false)
    private User userId; // 유저 정보

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market", referencedColumnName = "market", nullable = false)
    private Coin market; // market을 coins 테이블의 market을 참조하도록 설정

    @Column(name = "createdAt", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt; // 관심 코인 등록 시간

    // 기본 생성자
    public FavoriteCoin() {}

    // 생성자
    public FavoriteCoin(User userId, Coin coin) {
        this.id = new FavoriteCoinId(userId.getId(), coin.getMarket());  // market을 사용하여 복합키 설정
        this.userId = userId;
        this.market = coin;
    }
}
