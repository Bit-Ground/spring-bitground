package bit.bitgroundspring.util;

import bit.bitgroundspring.dto.response.Message;
import bit.bitgroundspring.dto.response.NotificationResponse;
import bit.bitgroundspring.entity.Notification;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserSseEmitters {
    private final NotificationService notificationService;
    
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
    
    public boolean sendToUser(Integer userId, NotificationResponse data) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return false;
        }
        
        List<SseEmitter> deadEmitters = new ArrayList<>();
        
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(data));
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
        
        // 알림 내역 저장
        Map<String, Object> dataMap = data.getData();
        if (data.getMessage() == Message.ORDER_EXECUTION) {
            String orderType = dataMap.get("orderType").equals("BUY") ? "매수" : "매도";
            String symbol = (String) dataMap.get("symbol");
            String cutSymbol = symbol.split("-")[1];
            Double amount = (Double) dataMap.get("amount");
            String tradePrice = (String) dataMap.get("tradePrice");
            String message = String.format("""
                    예약 %s 주문이 체결되었습니다.
                    수량 : %.7f %s
                    체결 : 개당 %s원
                    """, orderType, amount, cutSymbol, tradePrice);
            
            Notification notification = Notification.builder()
                    .user(User.builder().id(userId).build())
                    .messageType(data.getMessage())
                    .message(message)
                    .build();
            notificationService.saveNotification(notification);
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
        
        // 알림 내역 저장
        Map<String, Object> dataMap = request.getData();
        if (request.getMessage() == Message.SEASON_UPDATE) {
            String seasonName = (String) dataMap.get("seasonName");
            String seasonFlag = (String) dataMap.get("seasonFlag");
            String message = getMessage(seasonFlag, seasonName);
            Notification notification = Notification.builder()
                    .user(User.builder().id(1).build())
                    .messageType(request.getMessage())
                    .message(message)
                    .build();
            notificationService.saveNotification(notification);
            
        } else if (request.getMessage() == Message.NOTICE) {
            String title = (String) dataMap.get("title");
            String message = String.format("""
                    🔔 새로운 공지사항이 등록되었습니다.
                    공지사항 탭에서 확인해보세요.
                    [%s]
                    """, title);
            
            Notification notification = Notification.builder()
                    .user(User.builder().id(1).build())
                    .messageType(request.getMessage())
                    .message(message).build();
            notificationService.saveNotification(notification);
        }
        return results;
    }
    
    private String getMessage(String seasonFlag, String seasonName) {
        String message = "";
        if (seasonFlag.equals("season")) {
            message = String.format("""
                    🎉 %s 스플릿 1 🎉
                    새로운 시즌이 시작되었습니다!
                    이전 랭킹과 수익률을 확인해보세요.""", seasonName);
        } else if (seasonFlag.equals("split")) {
            message = String.format("""
                    🚀 %s 스플릿 2 🚀
                    새로운 스플릿이 시작되었습니다!
                    10,000,000원의 추가 자금이 지급됩니다.""", seasonName);
        }
        return message;
    }
}