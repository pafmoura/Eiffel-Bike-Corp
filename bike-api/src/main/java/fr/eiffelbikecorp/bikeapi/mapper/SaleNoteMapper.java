package fr.eiffelbikecorp.bikeapi.mapper;

import fr.eiffelbikecorp.bikeapi.domain.entity.SaleNote;
import fr.eiffelbikecorp.bikeapi.dto.response.SaleNoteResponse;

public final class SaleNoteMapper {
    private SaleNoteMapper() {}

    public static SaleNoteResponse toResponse(SaleNote n) {
        if (n == null) return null;
        return new SaleNoteResponse(
                n.getId(),
                n.getSaleOffer().getId(),
                n.getTitle(),
                n.getContent(),
                n.getCreatedAt(),
                n.getCreatedBy()
        );
    }
}
