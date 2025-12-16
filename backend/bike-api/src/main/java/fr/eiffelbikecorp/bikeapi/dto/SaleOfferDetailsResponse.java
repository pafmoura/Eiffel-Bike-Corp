package fr.eiffelbikecorp.bikeapi.dto;

import java.util.List;

public record SaleOfferDetailsResponse(
        SaleOfferResponse offer,
        List<SaleNoteResponse> notes
) {}
