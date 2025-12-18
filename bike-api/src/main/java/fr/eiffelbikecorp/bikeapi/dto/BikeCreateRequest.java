package fr.eiffelbikecorp.bikeapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record BikeCreateRequest(
        @NotBlank @Size(max = 255)
        String description,

        @NotNull
        ProviderType offeredByType,

        @NotNull
        UUID offeredById,

        @NotNull @DecimalMin(value = "0.00", inclusive = false)
        BigDecimal rentalDailyRateEur
) {
}
