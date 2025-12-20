package fr.eiffelbikecorp.bikeapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SalePaymentResponse(
        Long id,
        Long purchaseId,
        BigDecimal originalAmount,
        String originalCurrency,
        BigDecimal fxRateToEur,
        BigDecimal amountEur,
        String status,
        LocalDateTime paidAt,
        String stripePaymentIntentId
) {}
