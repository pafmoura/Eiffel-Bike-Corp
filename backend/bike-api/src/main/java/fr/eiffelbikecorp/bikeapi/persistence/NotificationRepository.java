package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // US_10 receive notification: query notifications for a customer via entry
    List<Notification> findByEntry_Customer_IdOrderBySentAtDesc(Long customerId);

    // audit trail on a specific waiting list entry
    List<Notification> findByEntry_IdOrderBySentAtAsc(Long entryId);
}
