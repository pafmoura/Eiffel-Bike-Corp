package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.dto.response.PurchaseResponse;

import java.util.List;
import java.util.UUID;

public interface PurchaseService {
    PurchaseResponse checkout(UUID customerId);
    PurchaseResponse getPurchase(UUID customerId, Long purchaseId);
    List<PurchaseResponse> listPurchases(UUID customerId);
}
