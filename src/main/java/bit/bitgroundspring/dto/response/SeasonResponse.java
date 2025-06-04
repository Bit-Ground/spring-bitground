package bit.bitgroundspring.dto.response;

import bit.bitgroundspring.dto.SeasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SeasonResponse {
    List<SeasonDto> seasons;
}
