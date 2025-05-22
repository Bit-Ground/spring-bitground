package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "coins")
public class Coin {

    @Id
    @Column(name = "market", nullable = false)
    private String market;  // 코인의 고유한 시장 정보 (예: "KRW-BTC", "USDT-ETH")

    // 생성자
    public Coin() {}

    public Coin(String market, String name, String symbol) {
        this.market = market;

    }
}
