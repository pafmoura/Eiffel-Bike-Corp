package fr.eiffelbikecorp.bikeapi.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record   SaleOfferResponse(
        Long id,
        Long bikeId,
        String status,
        BigDecimal askingPriceEur,
        LocalDateTime listedAt,
        LocalDateTime soldAt,
        String availability // "AVAILABLE" / "SOLD" etc (simple)
) {}
