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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserSseEmitters {
    private final NotificationService notificationService;
    
    private final Map<Integer, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    
    public void addUser(Integer userId, SseEmitter emitter) {
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        // 추가: 연결 관리 콜백
        emitter.onTimeout(() -> removeUser(userId, emitter));
        emitter.onCompletion(() -> removeUser(userId, emitter));
        emitter.onError((ex) -> removeUser(userId, emitter));
        
        log.debug("사용자 {} 연결됨. 총 연결: {}", userId, getUserConnectionCount(userId));
    }
    
    public boolean sendToUser(Integer userId, NotificationResponse data) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return false;
        }
        
        // 수정: 동시성 문제 해결
        List<SseEmitter> emittersCopy = new ArrayList<>(emitters);
        List<SseEmitter> deadEmitters = new ArrayList<>();
        
        for (SseEmitter emitter : emittersCopy) {
            try {
                emitter.send(SseEmitter.event().data(data));
            } catch (Exception e) {
                log.error("사용자 {}에게 데이터 전송 중 오류 발생: {}", userId, e.getMessage());
                deadEmitters.add(emitter);
            }
        }
        
        // 죽은 연결 정리
        if (!deadEmitters.isEmpty()) {
            List<SseEmitter> originalEmitters = userEmitters.get(userId);
            if (originalEmitters != null) {
                originalEmitters.removeAll(deadEmitters);
                if (originalEmitters.isEmpty()) {
                    userEmitters.remove(userId);
                }
            }
        }
        
        // 수정: 비동기 알림 저장
        CompletableFuture.runAsync(() -> saveNotification(userId, data, data.getMessage()));
        
        
        return !emittersCopy.isEmpty();
    }
    
    // 추가: 비동기 알림 저장 메서드
    private void saveNotification(Integer userId, NotificationResponse data, Message messageType) {
        try {
            Map<String, Object> dataMap = data.getData();
            String message = "";
            if (messageType == Message.ORDER_EXECUTION) {
                String orderType = dataMap.get("orderType").equals("BUY") ? "매수" : "매도";
                String symbol = (String) dataMap.get("symbol");
                String cutSymbol = symbol.split("-")[1];
                Float amount = (Float) dataMap.get("amount");
                String tradePrice = (String) dataMap.get("tradePrice");
                message = String.format("예약 %s 주문이 체결되었습니다.\n수량 : %.7f %s\n체결 : 개당 %s원",
                        orderType, amount, cutSymbol, tradePrice);
            } else if (messageType == Message.INQUIRY_UPDATE) {
                String title = (String) dataMap.get("title");
                message = String.format("작성하신 문의사항에 답변이 등록되었습니다.\n지금 바로 확인해보세요.\n📩[%s]", title);
            }
            Notification notification = Notification.builder()
                    .user(User.builder().id(userId).build())
                    .messageType(messageType)
                    .message(message)
                    .build();
            notificationService.saveNotification(notification);
            
        } catch (Exception e) {
            log.error("알림 저장 실패 - 사용자: {}, 오류: {}", userId, e.getMessage());
        }
    }
    
    // 수정: 병렬 처리로 성능 개선
    public Map<String, Integer> sendToAll(NotificationResponse request) {
        Set<Integer> userIds = getOnlineUsers();
        
        // 병렬 스트림 사용으로 성능 개선
        Map<Boolean, List<Integer>> results = userIds.parallelStream()
                .collect(Collectors.partitioningBy(userId -> sendToUser(userId, request)));
        
        int successCount = results.get(true).size();
        int failureCount = results.get(false).size();
        
        Map<String, Integer> result = new HashMap<>();
        result.put("success", successCount);
        result.put("failure", failureCount);
        
        log.info("모든 사용자에게 알림 전송 완료. 성공: {}, 실패: {}", successCount, failureCount);
        
        // ✅ 수정: 시스템 알림 비동기 저장
        if (request.getMessage() == Message.SEASON_UPDATE || request.getMessage() == Message.NOTICE) {
            CompletableFuture.runAsync(() -> saveSystemNotification(request));
        }
        
        return result;
    }
    
    // 추가: 시스템 알림 저장
    private void saveSystemNotification(NotificationResponse request) {
        try {
            Map<String, Object> dataMap = request.getData();
            String message = "";
            
            if (request.getMessage() == Message.SEASON_UPDATE) {
                String seasonName = (String) dataMap.get("seasonName");
                String seasonFlag = (String) dataMap.get("seasonFlag");
                message = getMessage(seasonFlag, seasonName);
            } else if (request.getMessage() == Message.NOTICE) {
                String title = (String) dataMap.get("title");
                message = String.format("🔔 새로운 공지사항이 등록되었습니다.\n고객센터 탭에서 확인해보세요.\n[%s]", title);
            }
            
            Notification notification = Notification.builder()
                    .user(User.builder().id(1).build())
                    .messageType(request.getMessage())
                    .message(message)
                    .build();
            notificationService.saveNotification(notification);
            
        } catch (Exception e) {
            log.error("시스템 알림 저장 실패: {}", e.getMessage());
        }
    }
    
    // 기존 메서드들...
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
    
    public Set<Integer> getOnlineUsers() {
        return userEmitters.keySet();
    }
    
    public int getUserConnectionCount(Integer userId) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        return emitters != null ? emitters.size() : 0;
    }
    
    private String getMessage(String seasonFlag, String seasonName) {
        if (seasonFlag.equals("season")) {
            return String.format("🎉 %s 스플릿 1 🎉\n새로운 시즌이 시작되었습니다!\n이전 랭킹과 수익률을 확인해보세요.", seasonName);
        } else if (seasonFlag.equals("split")) {
            return String.format("🚀 %s 스플릿 2 🚀\n새로운 스플릿이 시작되었습니다!\n10,000,000원의 추가 자금이 지급됩니다.", seasonName);
        }
        return "";
    }
}