package fr.eiffelbikecorp.bikeapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BasketItemResponse(
        Long id,
        Long saleOfferId,
        Long bikeId,
        BigDecimal unitPriceEurSnapshot,
        LocalDateTime addedAt
) {}
