package fr.eiffelbikecorp.bikeapi.dto.response;

import java.math.BigDecimal;

public record PurchaseItemResponse(
        Long id,
        Long saleOfferId,
        Long bikeId,
        BigDecimal unitPriceEurSnapshot
) {}
