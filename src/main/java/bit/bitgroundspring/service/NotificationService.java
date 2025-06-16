package bit.bitgroundspring.service;

import bit.bitgroundspring.entity.Notification;
import bit.bitgroundspring.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    
    // 알림 저장
    public void saveNotification(Notification notification) {
        notificationRepository.save(notification);
    }
}
