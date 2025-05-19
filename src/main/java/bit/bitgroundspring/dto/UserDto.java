package bit.bitgroundspring.dto;

import bit.bitgroundspring.entity.UserRole;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
    
    private UserRole role;
    
}