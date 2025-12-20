package fr.eiffelbikecorp.bikeapi.mapper;

import fr.eiffelbikecorp.bikeapi.domain.SaleOffer;
import fr.eiffelbikecorp.bikeapi.dto.SaleOfferResponse;

public final class SaleOfferMapper {
    private SaleOfferMapper() {}

    public static SaleOfferResponse toResponse(SaleOffer o) {
        return new SaleOfferResponse(
                o.getId(),
                o.getBike().getId(),
                o.getStatus().name(),
                o.getAskingPriceEur(),
                o.getListedAt(),
                o.getSoldAt(),
                o.getStatus().name().equals("LISTED") ? "AVAILABLE" : o.getStatus().name()
        );
    }
}
