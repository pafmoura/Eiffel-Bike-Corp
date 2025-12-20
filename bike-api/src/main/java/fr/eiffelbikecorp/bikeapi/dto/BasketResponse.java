package fr.eiffelbikecorp.bikeapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BasketResponse(
        Long id,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<BasketItemResponse> items
) {}
