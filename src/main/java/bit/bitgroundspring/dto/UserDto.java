package bit.bitgroundspring.dto;

import bit.bitgroundspring.entity.Role;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private int id;
    
    private String provider;    // oauth2 제공자 (google, naver, kakao)
    
    private String providerId;  // oauth2 제공자의 subject id
    
    private String name;
    
    private String email;
    
    private String profileImage;
    
    private Role role;
    
    private Integer cash; // 사용자의 현금 자산
    
}