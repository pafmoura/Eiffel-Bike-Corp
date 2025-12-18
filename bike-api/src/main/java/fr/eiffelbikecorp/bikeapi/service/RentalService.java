package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.dto.*;

import java.util.List;
import java.util.UUID;

public interface RentalService {

    RentBikeResultResponse rentBikeOrJoinWaitingList(RentBikeRequest request);

    ReturnBikeResponse returnBike(Long rentalId, ReturnBikeRequest request);

    List<NotificationResponse> listMyNotifications(UUID customerId);
}
