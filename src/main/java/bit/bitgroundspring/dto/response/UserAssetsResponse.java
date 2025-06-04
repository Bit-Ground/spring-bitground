package bit.bitgroundspring.dto.response;

import bit.bitgroundspring.dto.UserAssetDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAssetsResponse {
    private Integer cash;
    private List<UserAssetDto> userAssets;
}