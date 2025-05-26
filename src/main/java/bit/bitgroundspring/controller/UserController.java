package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.UserDto;
import bit.bitgroundspring.dto.UserUpdate;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.security.oauth2.AuthService;
import bit.bitgroundspring.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AuthService authService;

    //사용자 정보 조회
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @CookieValue(name = "jwt_token", required = false) String jwtToken) {

        // 인증된 사용자의 정보를 가져옵니다.
        UserDto userDto = authService.getUserInfoFromToken(jwtToken);
        String provider = userDto.getProvider();
        String providerId = userDto.getProviderId();

        // 사용자 정보를 DB에서 조회합니다.
        Optional<User> user = userService.getUserBySocialId(provider, providerId);

        if (user.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "사용자 정보가 존재하지 않습니다."
            ));
        }

        userDto = UserDto.builder()
                .id(user.get().getId())
                .name(user.get().getName())
                .email(user.get().getEmail())
                .profileImage(user.get().getProfileImage())
                .provider(user.get().getProvider())
                .role(user.get().getRole())
                .build();

        // 사용자 정보를 Map 형태로 변환합니다.
        Map<String, Object> userInfo = Map.of(
                "success", true,
                "message", "사용자 정보 조회 성공",
                "user", userDto
        );
        return ResponseEntity.ok(userInfo);
    }

    //사용자 정보 수정
    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateCurrentUser(
            @CookieValue(name = "jwt_token", required = false) String jwtToken,
            @RequestBody UserUpdate request) {
        // 1. 인증 정보 확인
        UserDto userDto = authService.getUserInfoFromToken(jwtToken);
        String provider = userDto.getProvider();
        String providerId = userDto.getProviderId();

        // 2. 유저 조회
        Optional<User> userOpt = userService.getUserBySocialId(provider, providerId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "수정 대상 사용자를 찾을 수 없습니다."
            ));
    }
        User user = userOpt.get();

        // 3. 수정 처리
        User updated = userService.updateUser(user.getId(), request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "사용자 정보 수정 성공",
                "user", UserDto.builder()
                        .id(updated.getId())
                        .name(updated.getName())
                        .email(updated.getEmail())
                        .profileImage(updated.getProfileImage())
                        .provider(updated.getProvider())
                        .role(updated.getRole())
                        .build()
        ));
}

    //사용자 탈퇴
}