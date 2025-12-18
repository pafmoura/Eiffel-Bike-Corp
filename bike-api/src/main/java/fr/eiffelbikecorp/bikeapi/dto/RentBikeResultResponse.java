package fr.eiffelbikecorp.bikeapi.dto;

public record RentBikeResultResponse(
        RentResult result,
        Long rentalId,
        Long waitingListEntryId,
        String message
) {
}
