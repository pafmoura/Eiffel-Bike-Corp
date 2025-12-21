package fr.eiffelbikecorp.bikeapi.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RentalResponse(
        Long id,
        Long bikeId,
        UUID customerId,
        String status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        BigDecimal totalAmountEur
) {
}
