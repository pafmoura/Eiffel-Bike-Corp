package fr.eiffelbikecorp.bikeapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PayPurchaseRequest(
        @NotNull Long purchaseId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String currency,
        @Schema(example = "pm_card_visa")
        @NotBlank String paymentMethodId
) {}
