package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.dto.request.RentBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.ReturnBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface RentalService {

    RentBikeResultResponse rentBikeOrJoinWaitingList(RentBikeRequest request);

    ReturnBikeResponse returnBike(Long rentalId, ReturnBikeRequest request);

    List<NotificationResponse> listMyNotifications(UUID customerId);

    List<RentBikeResultResponse> findActiveRentalsByCustomer(UUID customerId);

    List<NotificationResponse> findWaitlistByCustomer(UUID customerId);

    List<ActiveBikeResponse> findMyActiveBikeIds(UUID customerId);

    List<RentalResponse> listMyRentals(UUID customerId);
}
