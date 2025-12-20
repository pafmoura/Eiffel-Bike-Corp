package fr.eiffelbikecorp.bikeapi.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PayPurchaseRequest(
        @NotNull Long purchaseId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String paymentMethodId
) {}
