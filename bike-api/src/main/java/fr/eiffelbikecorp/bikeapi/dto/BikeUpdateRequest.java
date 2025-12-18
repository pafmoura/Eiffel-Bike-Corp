package fr.eiffelbikecorp.bikeapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BikeUpdateRequest(
        @Size(max = 255)
        String description,

        String status, // "AVAILABLE", "RENTED", "MAINTENANCE"
        @DecimalMin(value = "0.00", inclusive = false)
        BigDecimal rentalDailyRateEur
) {
}
