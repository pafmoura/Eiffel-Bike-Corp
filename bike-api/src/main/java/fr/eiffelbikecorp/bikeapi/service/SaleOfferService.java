package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.dto.request.CreateSaleNoteRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.CreateSaleOfferRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.SaleNoteResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.SaleOfferDetailsResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.SaleOfferResponse;

import java.util.List;

public interface SaleOfferService {
    SaleOfferResponse createSaleOffer(CreateSaleOfferRequest request);
    SaleNoteResponse addSaleNote(CreateSaleNoteRequest request);

    List<SaleOfferResponse> searchSaleOffers(String q);

    SaleOfferDetailsResponse getSaleOfferDetailsByBike(Long bikeId);
    SaleOfferDetailsResponse getSaleOfferDetails(Long saleOfferId);
}
