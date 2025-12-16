package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.dto.*;

import java.util.List;

public interface SaleService {

    // US_04 list bike for sale
    SaleOfferResponse createSaleOffer(CreateSaleOfferRequest request);

    // US_04 add notes to sale
    SaleNoteResponse addSaleNote(CreateSaleNoteRequest request);

    // US_11 search bikes to buy
    List<SaleOfferResponse> searchSaleOffers(String q);

    // US_12 view notes associated with a bike offered for sale
    SaleOfferDetailsResponse getSaleOfferDetailsByBike(Long bikeId);

    // Alternative: details by offer id
    SaleOfferDetailsResponse getSaleOfferDetails(Long saleOfferId);
}
