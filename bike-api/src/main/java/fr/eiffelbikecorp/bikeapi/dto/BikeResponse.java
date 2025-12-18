package fr.eiffelbikecorp.bikeapi.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BikeResponse(
        Long id,
        String description,
        String status,
        ProviderRef offeredBy,
        BigDecimal rentalDailyRateEur
) {
}
