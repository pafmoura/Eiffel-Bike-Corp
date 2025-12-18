package fr.eiffelbikecorp.bikeapi.mapper;

import fr.eiffelbikecorp.bikeapi.domain.Notification;
import fr.eiffelbikecorp.bikeapi.dto.NotificationResponse;

public final class NotificationMapper {
    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification n) {
        if (n == null) return null;
        var entry = n.getEntry();
        var bikeId = entry.getWaitingList().getBike().getId();
        var customerId = entry.getCustomer().getId();
        return new NotificationResponse(
                n.getId(),
                customerId,
                bikeId,
                n.getMessage(),
                n.getSentAt()
        );
    }
}
