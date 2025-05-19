package bit.bitgroundspring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteCoinId implements Serializable {

    @Column(name = "userId")
    private Integer userId;

    @Column(name = "market", insertable = false, updatable = false)
    private String market;
}
