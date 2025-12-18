package fr.eiffelbikecorp.bikeapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SaleOfferResponse(
        Long id,
        Long bikeId,
        UUID sellerCorpId,
        BigDecimal askingPriceEur,
        String status,
        LocalDateTime listedAt,
        LocalDateTime soldAt,
        UUID buyerCustomerId
) {}
