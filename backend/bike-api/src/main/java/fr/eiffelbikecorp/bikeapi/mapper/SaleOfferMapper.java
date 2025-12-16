package fr.eiffelbikecorp.bikeapi.mapper;

import fr.eiffelbikecorp.bikeapi.domain.SaleOffer;
import fr.eiffelbikecorp.bikeapi.dto.SaleOfferResponse;

public final class SaleOfferMapper {
    private SaleOfferMapper() {}

    public static SaleOfferResponse toResponse(SaleOffer o) {
        if (o == null) return null;
        return new SaleOfferResponse(
                o.getId(),
                o.getBike().getId(),
                o.getSeller().getId(),
                o.getAskingPriceEur(),
                o.getStatus().name(),
                o.getListedAt(),
                o.getSoldAt(),
                o.getBuyer() != null ? o.getBuyer().getId() : null
        );
    }
}
