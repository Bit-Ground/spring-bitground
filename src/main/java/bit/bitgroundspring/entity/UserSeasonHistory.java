package bit.bitgroundspring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@Setter
@Table(name = "users_seasons_history")
public class UserSeasonHistory {

    @EmbeddedId
    private UserSeasonHistoryId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "userId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("seasonId")
    @JoinColumn(name = "seasonId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Season season;

    @Column(name = "tier", nullable = false, length = 30)
    private String tier;

    @Column(name = "rank", nullable = false)
    private int rank;

    @Column(name = "profitRate", nullable = false)
    private float profitRate;

    // 기본 생성자
    public UserSeasonHistory() {}

    // 생성자, getter, setter
    public UserSeasonHistory(User user, Season season, String tier, int rank, float profitRate) {
        this.id = new UserSeasonHistoryId(user.getId(), season.getId());
        this.user = user;
        this.season = season;
        this.tier = tier;
        this.rank = rank;
        this.profitRate = profitRate;
    }

    public UserSeasonHistoryId getId() { return id; }
    public void setId(UserSeasonHistoryId id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Season getSeason() { return season; }
    public void setSeason(Season season) { this.season = season; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public float getProfitRate() { return profitRate; }
    public void setProfitRate(float profitRate) { this.profitRate = profitRate; }
}