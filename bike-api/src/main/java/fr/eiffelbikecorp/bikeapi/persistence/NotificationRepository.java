package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // US_10 receive notification: query notifications for a customer via entry
    List<Notification> findByEntry_Customer_IdOrderBySentAtDesc(UUID customerId);

    // audit trail on a specific waiting list entry
    List<Notification> findByEntry_IdOrderBySentAtAsc(Long entryId);
}
