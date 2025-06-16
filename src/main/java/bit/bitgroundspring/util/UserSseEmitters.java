package bit.bitgroundspring.util;

import bit.bitgroundspring.dto.response.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class UserSseEmitters {
    // 사용자별 SseEmitter 리스트 관리 (한 유저가 여러 탭/디바이스 사용 가능)
    private final Map<Integer, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    
    public void addUser(Integer userId, SseEmitter emitter) {
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.debug("사용자 {} 연결됨. 총 연결: {}", userId, getUserConnectionCount(userId));
    }
    
    public void removeUser(Integer userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
        log.debug("사용자 {} 연결 해제됨", userId);
    }
    
    public boolean sendToUser(Integer userId, Object data) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return false;
        }
        
        List<SseEmitter> deadEmitters = new ArrayList<>();
        
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .data(data));
            } catch (Exception e) {
                log.error("사용자 {}에게 데이터 전송 중 오류 발생: {}", userId, e.getMessage());
                deadEmitters.add(emitter);
                try {
                    emitter.complete(); // 여기서 명시적으로 complete 호출
                } catch (Exception ce) {
                    log.error("사용자 {}의 SseEmitter 완료 중 오류 발생: {}", userId, ce.getMessage());
                }
            }
        }
        
        // 죽은 연결 정리
        emitters.removeAll(deadEmitters);
        if (emitters.isEmpty()) {
            userEmitters.remove(userId);
        }
        
        return !emitters.isEmpty();
    }
    
    public Set<Integer> getOnlineUsers() {
        return userEmitters.keySet();
    }
    
    public int getUserConnectionCount(Integer userId) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        return emitters != null ? emitters.size() : 0;
    }
    
    public Map<String, Integer> sendToAll(NotificationResponse request) {
        Set<Integer> userIds = getOnlineUsers();
        Map<String, Integer> results = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;
        for (Integer userId : userIds) {
            boolean sent = sendToUser(userId, request);
            if (sent) successCount++;
            else failureCount++;
        }
        
        results.put("success", successCount);
        results.put("failure", failureCount);
        
        log.info("모든 사용자에게 알림 전송 완료. 성공: {}, 실패: {}", successCount, failureCount);
        return results;
    }
}