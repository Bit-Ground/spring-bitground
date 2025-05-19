package bit.bitgroundspring.security.oauth2;

import bit.bitgroundspring.dto.UserDto;
import bit.bitgroundspring.entity.UserRole;
import bit.bitgroundspring.security.token.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }
    
    // 토큰에서 사용자 정보 추출
    public UserDto getUserInfoFromToken(String token) {
        Claims claims = jwtTokenProvider.getClaimsFromToken(token);
        return UserDto.builder()
                .id(claims.get("userId", Integer.class))
                .provider(claims.get("provider", String.class))
                .providerId(claims.getSubject())
                .role(claims.get("role", UserRole.class))
                .build();
    }
}

