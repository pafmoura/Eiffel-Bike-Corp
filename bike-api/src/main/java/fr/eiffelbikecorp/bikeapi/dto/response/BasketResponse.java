package fr.eiffelbikecorp.bikeapi.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record BasketResponse(
        Long id,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<BasketItemResponse> items
) {}
