package bit.bitgroundspring.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class FavoriteCoinId implements Serializable {

    private Integer userId;
    private String market;  // Coin 엔티티의 market을 사용

    public FavoriteCoinId() {}

    public FavoriteCoinId(Integer userId, String market) {
        this.userId = userId;
        this.market = market;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FavoriteCoinId)) return false;
        FavoriteCoinId that = (FavoriteCoinId) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(market, that.market);  // market 비교
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, market);  // market 기준으로 hashcode
    }

    // getter & setter
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }
}
