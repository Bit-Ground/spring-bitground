package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.response.NotificationResponse;
import bit.bitgroundspring.security.oauth2.AuthService;
import bit.bitgroundspring.util.UserSseEmitters;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {
    
    private final UserSseEmitters userSseEmitters;
    private final AuthService authService;
    
    // 사용자별 SSE 구독 엔드포인트
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@CookieValue(name = "jwt_token", required = false) String jwtToken) {
        // JWT 토큰에서 사용자 ID 추출
        Integer userId = authService.getUserIdFromToken(jwtToken);
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 무제한 대기 시간 설정
        userSseEmitters.addUser(userId, emitter);
        
        // 연결 종료 시 정리
        emitter.onCompletion(() -> userSseEmitters.removeUser(userId, emitter));
        emitter.onTimeout(() -> userSseEmitters.removeUser(userId, emitter));
        emitter.onError((e) -> userSseEmitters.removeUser(userId, emitter));
        
        return emitter;
    }
    
    // 여러 유저에게 알림 전송
    @PostMapping("/send")
    public ResponseEntity<Map<String, Integer>> sendToMultipleUsers(
            @RequestBody NotificationResponse request) {
        Map<String, Integer> results = userSseEmitters.sendToAll(request); // 모든 사용자에게 알림 전송
        return ResponseEntity.ok(results);
    }
}
