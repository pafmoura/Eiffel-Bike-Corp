package fr.eiffelbikecorp.bikeapi.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseResponse(
        Long id,
        String status,
        BigDecimal totalAmountEur,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        List<PurchaseItemResponse> items
) {}
