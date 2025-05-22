package bit.bitgroundspring.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserSeasonHistoryId implements Serializable {

    private Integer userId;
    private Integer seasonId;

    public UserSeasonHistoryId() {}

    public UserSeasonHistoryId(Integer userId, Integer seasonId) {
        this.userId = userId;
        this.seasonId = seasonId;
    }

    // equals & hashCode는 반드시 오버라이드해야 함
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserSeasonHistoryId)) return false;
        UserSeasonHistoryId that = (UserSeasonHistoryId) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(seasonId, that.seasonId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, seasonId);
    }

    // getter & setter
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getSeasonId() { return seasonId; }
    public void setSeasonId(Integer seasonId) { this.seasonId = seasonId; }
}
