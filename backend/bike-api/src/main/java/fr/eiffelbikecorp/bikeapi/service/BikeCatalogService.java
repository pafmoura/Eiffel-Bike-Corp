package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.dto.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.BikeUpdateRequest;

import java.util.List;

public interface BikeCatalogService {

    BikeResponse offerBikeForRent(BikeCreateRequest request);

    BikeResponse updateBike(Long bikeId, BikeUpdateRequest request);

    List<BikeResponse> searchBikesToRent(String status, String q, Long offeredById);
}
