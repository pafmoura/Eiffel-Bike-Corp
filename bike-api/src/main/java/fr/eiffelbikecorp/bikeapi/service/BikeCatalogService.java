package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.dto.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.BikeUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface BikeCatalogService {

    List<BikeResponse> findAll();

    BikeResponse offerBikeForRent(BikeCreateRequest request);

    BikeResponse updateBike(Long bikeId, BikeUpdateRequest request);

    List<BikeResponse> searchBikesToRent(String status, String q, UUID offeredById);
}
