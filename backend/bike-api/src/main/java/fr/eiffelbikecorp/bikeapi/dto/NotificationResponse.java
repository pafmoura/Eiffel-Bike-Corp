package fr.eiffelbikecorp.bikeapi.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long customerId,
        Long bikeId,
        String message,
        LocalDateTime sentAt
) {
}
