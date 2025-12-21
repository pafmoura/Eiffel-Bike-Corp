package fr.eiffelbikecorp.bikeapi.dto.response;

import fr.eiffelbikecorp.bikeapi.dto.ProviderRef;

import java.math.BigDecimal;

public record BikeResponse(
        Long id,
        String description,
        String status,
        ProviderRef offeredBy,
        BigDecimal rentalDailyRateEur
) {
}
