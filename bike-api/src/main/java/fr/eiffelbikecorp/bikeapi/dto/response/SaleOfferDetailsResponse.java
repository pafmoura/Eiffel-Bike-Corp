package fr.eiffelbikecorp.bikeapi.dto.response;

import java.util.List;

public record SaleOfferDetailsResponse(
        SaleOfferResponse offer,
        List<SaleNoteResponse> notes
) {}
