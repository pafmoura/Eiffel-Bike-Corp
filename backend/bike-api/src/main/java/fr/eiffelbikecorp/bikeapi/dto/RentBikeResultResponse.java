package fr.eiffelbikecorp.bikeapi.dto;

public record RentBikeResultResponse(
        RentResult result,          // RENTED or WAITLISTED
        Long rentalId,
        Long waitingListEntryId,
        String message
) {
}
