package fr.eiffelbikecorp.bikeapi.dto.response;

public record ReturnBikeResponse(
        RentalResponse closedRental,
        RentalResponse nextRental,                 // null if nobody was waiting
        NotificationResponse notificationSent       // null if nobody was waiting
) {
}
