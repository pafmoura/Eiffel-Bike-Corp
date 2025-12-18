package fr.eiffelbikecorp.bikeapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record PayRentalRequest<paymentMethodId>(
        @NotNull Long rentalId,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        BigDecimal amount,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code like USD, BRL, EUR")
        String currency,

        @NotBlank
        String paymentMethodId
) {
}
