package fr.eiffelbikecorp.bikeapi.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateSaleOfferRequest(
        @NotNull Long bikeId,
        @NotNull UUID sellerCorpId,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        BigDecimal askingPriceEur
) {}
