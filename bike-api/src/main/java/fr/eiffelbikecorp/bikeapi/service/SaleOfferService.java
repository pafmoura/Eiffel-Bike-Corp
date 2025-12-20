package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.dto.*;

import java.util.List;

public interface SaleOfferService {
    SaleOfferResponse createSaleOffer(CreateSaleOfferRequest request);
    SaleNoteResponse addSaleNote(CreateSaleNoteRequest request);

    List<SaleOfferResponse> searchSaleOffers(String q);

    SaleOfferDetailsResponse getSaleOfferDetailsByBike(Long bikeId);
    SaleOfferDetailsResponse getSaleOfferDetails(Long saleOfferId);
}
