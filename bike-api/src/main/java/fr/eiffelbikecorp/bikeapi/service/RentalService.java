package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.dto.request.RentBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.ReturnBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.ActiveBikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.NotificationResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.RentBikeResultResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.ReturnBikeResponse;

import java.util.List;
import java.util.UUID;

public interface RentalService {

    RentBikeResultResponse rentBikeOrJoinWaitingList(RentBikeRequest request);

    ReturnBikeResponse returnBike(Long rentalId, ReturnBikeRequest request);

    List<NotificationResponse> listMyNotifications(UUID customerId);


    List<RentBikeResultResponse> findActiveRentalsByCustomer(UUID customerId);


    List<NotificationResponse> findWaitlistByCustomer(UUID customerId);

    List<ActiveBikeResponse> findMyActiveBikeIds(UUID customerId);

    }
