package fr.eiffelbikecorp.bikeapi.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateSaleOfferRequest(
        @NotNull Long bikeId,
        @NotNull Long sellerCorpId,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        BigDecimal askingPriceEur
) {}
