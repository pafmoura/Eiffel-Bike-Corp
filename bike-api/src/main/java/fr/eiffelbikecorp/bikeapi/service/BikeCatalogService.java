package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.dto.request.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeUpdateRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.ReturnNoteResponse;

import java.util.List;
import java.util.UUID;

public interface BikeCatalogService {

    List<BikeResponse> findAll();

    BikeResponse offerBikeForRent(BikeCreateRequest request);

    BikeResponse updateBike(Long bikeId, BikeUpdateRequest request);

    List<BikeResponse> searchBikesToRent(String status, String q, UUID offeredById);

     List<ReturnNoteResponse> getReturnNotesForBike(Long bikeId);

    }
