package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.dto.*;

import java.util.List;

public interface RentalService {

    RentBikeResultResponse rentBikeOrJoinWaitingList(RentBikeRequest request);

    ReturnBikeResponse returnBike(Long rentalId, ReturnBikeRequest request);

    List<NotificationResponse> listMyNotifications(Long customerId);
}
