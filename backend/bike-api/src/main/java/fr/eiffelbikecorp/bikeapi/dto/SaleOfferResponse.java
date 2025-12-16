package fr.eiffelbikecorp.bikeapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleOfferResponse(
        Long id,
        Long bikeId,
        Long sellerCorpId,
        BigDecimal askingPriceEur,
        String status,
        LocalDateTime listedAt,
        LocalDateTime soldAt,
        Long buyerCustomerId
) {}
