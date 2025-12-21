package fr.eiffelbikecorp.bikeapi.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RentalPaymentResponse(
        Long id,
        Long rentalId,
        BigDecimal originalAmount,
        String originalCurrency,
        BigDecimal fxRateToEur,
        BigDecimal amountEur,
        String status,
        LocalDateTime paidAt
) {}
