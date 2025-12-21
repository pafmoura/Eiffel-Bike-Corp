package fr.eiffelbikecorp.bikeapi.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        Long id,
        UUID customerId,
        Long bikeId,
        String message,
        LocalDateTime sentAt
) {
}
