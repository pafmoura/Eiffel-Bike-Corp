package fr.eiffelbikecorp.bikeapi.dto.response;

import java.time.LocalDateTime;

public record SaleNoteResponse(
        Long id,
        Long saleOfferId,
        String title,
        String content,
        LocalDateTime createdAt,
        String createdBy
) {}
