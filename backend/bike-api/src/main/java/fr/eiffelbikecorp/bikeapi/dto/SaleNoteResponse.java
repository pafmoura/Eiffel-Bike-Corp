package fr.eiffelbikecorp.bikeapi.dto;

import java.time.LocalDateTime;

public record SaleNoteResponse(
        Long id,
        Long saleOfferId,
        String title,
        String content,
        LocalDateTime createdAt,
        String createdBy
) {}
