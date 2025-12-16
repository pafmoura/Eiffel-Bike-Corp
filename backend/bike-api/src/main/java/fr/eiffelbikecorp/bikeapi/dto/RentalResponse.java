package fr.eiffelbikecorp.bikeapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RentalResponse(
        Long id,
        Long bikeId,
        Long customerId,
        String status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        BigDecimal totalAmountEur
) {
}
