package bit.bitgroundspring.entity;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAssetId implements Serializable {
    private Integer userId;
    private String market;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserAssetId)) return false;
        UserAssetId that = (UserAssetId) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(market, that.market);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, market);
    }
}