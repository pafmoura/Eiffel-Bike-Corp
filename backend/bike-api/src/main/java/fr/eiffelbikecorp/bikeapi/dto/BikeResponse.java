package fr.eiffelbikecorp.bikeapi.dto;

import java.math.BigDecimal;

public record BikeResponse(
        Long id,
        String description,
        String status,
        ProviderRef offeredBy,
        BigDecimal rentalDailyRateEur
) {
}
