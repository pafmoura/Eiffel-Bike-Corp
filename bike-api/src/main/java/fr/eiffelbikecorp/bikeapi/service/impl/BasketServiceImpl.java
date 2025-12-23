package fr.eiffelbikecorp.bikeapi.service.impl;

import fr.eiffelbikecorp.bikeapi.domain.entity.Basket;
import fr.eiffelbikecorp.bikeapi.domain.entity.BasketItem;
import fr.eiffelbikecorp.bikeapi.domain.entity.SaleOffer;
import fr.eiffelbikecorp.bikeapi.domain.enums.BasketStatus;
import fr.eiffelbikecorp.bikeapi.domain.enums.SaleOfferStatus;
import fr.eiffelbikecorp.bikeapi.dto.request.AddToBasketRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.BasketResponse;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import fr.eiffelbikecorp.bikeapi.mapper.BasketMapper;
import fr.eiffelbikecorp.bikeapi.persistence.BasketItemRepository;
import fr.eiffelbikecorp.bikeapi.persistence.BasketRepository;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.SaleOfferRepository;
import fr.eiffelbikecorp.bikeapi.service.BasketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BasketServiceImpl implements BasketService {

    private final CustomerRepository customerRepository;
    private final BasketRepository basketRepository;
    private final BasketItemRepository basketItemRepository;
    private final SaleOfferRepository saleOfferRepository;

    @Override
    @Transactional
    public BasketResponse getOrCreateOpenBasket(UUID customerId) {
        ensureCustomerExists(customerId);
        Basket basket = basketRepository.findByCustomer_IdAndStatus(customerId, BasketStatus.OPEN)
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    Basket b = Basket.builder()
                            .customer(customerRepository.getReferenceById(customerId))
                            .status(BasketStatus.OPEN)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    return basketRepository.save(b);
                });
        // Load items (if LAZY)
        basket.getItems().size();
        return BasketMapper.toResponse(basket);
    }

    @Override
    @Transactional
    public BasketResponse addItem(UUID customerId, AddToBasketRequest request) {
        ensureCustomerExists(customerId);
        Basket basket = basketRepository.findByCustomer_IdAndStatus(customerId, BasketStatus.OPEN)
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    Basket b = Basket.builder()
                            .customer(customerRepository.getReferenceById(customerId))
                            .status(BasketStatus.OPEN)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    return basketRepository.save(b);
                });
        SaleOffer offer = saleOfferRepository.findById(request.saleOfferId())
                .orElseThrow(() -> new NotFoundException("SaleOffer not found: " + request.saleOfferId()));
        if (offer.getStatus() != SaleOfferStatus.LISTED) {
            throw new BusinessRuleException("Offer is not available: " + offer.getId());
        }
        if (basketItemRepository.existsByBasket_IdAndOffer_Id(basket.getId(), offer.getId())) {
            throw new BusinessRuleException("Offer already in basket: " + offer.getId());
        }
        BasketItem item = BasketItem.builder()
                .basket(basket)
                .offer(offer)
                .unitPriceEurSnapshot(offer.getAskingPriceEur())
                .addedAt(LocalDateTime.now())
                .build();
        basket.getItems().add(item);
        basket.setUpdatedAt(LocalDateTime.now());
        basketRepository.save(basket);
        return BasketMapper.toResponse(basket);
    }

    @Override
    @Transactional
    public BasketResponse removeItem(UUID customerId, Long saleOfferId) {
        Basket basket = basketRepository.findByCustomer_IdAndStatus(customerId, BasketStatus.OPEN)
                .orElseThrow(() -> new NotFoundException("Open basket not found for customer: " + customerId));
        boolean removed = basket.removeOffer(saleOfferId);
        if (!removed) {
            throw new NotFoundException("BasketItem not found for offer: " + saleOfferId);
        }
        Basket saved = basketRepository.save(basket);
        return BasketMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BasketResponse clear(UUID customerId) {
        ensureCustomerExists(customerId);
        Basket basket = basketRepository.findByCustomer_IdAndStatus(customerId, BasketStatus.OPEN)
                .orElseThrow(() -> new NotFoundException("Open basket not found for customer: " + customerId));
        basket.getItems().clear();
        basket.setUpdatedAt(LocalDateTime.now());
        Basket saved = basketRepository.save(basket);
        return BasketMapper.toResponse(saved);
    }

    private void ensureCustomerExists(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new NotFoundException("Customer not found: " + customerId);
        }
    }
}
