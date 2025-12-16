package fr.eiffelbikecorp.bikeapi.mapper;

import fr.eiffelbikecorp.bikeapi.domain.SaleNote;
import fr.eiffelbikecorp.bikeapi.dto.SaleNoteResponse;

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
