package fr.eiffelbikecorp.bikeapi.dto.response;

import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;

public record RentBikeResultResponse(
        RentResult result,
        Long rentalId,
        Long waitingListEntryId,
        String message
) {
}
