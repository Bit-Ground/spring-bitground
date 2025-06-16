package bit.bitgroundspring.repository;

import bit.bitgroundspring.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

}
