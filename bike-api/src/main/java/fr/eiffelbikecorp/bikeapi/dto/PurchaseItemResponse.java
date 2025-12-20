package fr.eiffelbikecorp.bikeapi.dto;

import java.math.BigDecimal;

public record PurchaseItemResponse(
        Long id,
        Long saleOfferId,
        Long bikeId,
        BigDecimal unitPriceEurSnapshot
) {}
