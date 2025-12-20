package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.dto.AddToBasketRequest;
import fr.eiffelbikecorp.bikeapi.dto.BasketResponse;

import java.util.UUID;

public interface BasketService {
    BasketResponse getOrCreateOpenBasket(UUID customerId);
    BasketResponse addItem(UUID customerId, AddToBasketRequest request);
    BasketResponse removeItem(UUID customerId, Long saleOfferId);
    BasketResponse clear(UUID customerId);
}
