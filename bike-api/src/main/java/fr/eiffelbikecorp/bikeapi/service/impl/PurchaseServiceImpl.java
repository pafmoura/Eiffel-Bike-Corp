package fr.eiffelbikecorp.bikeapi.service.impl;

import fr.eiffelbikecorp.bikeapi.domain.entity.*;
import fr.eiffelbikecorp.bikeapi.domain.enums.BasketStatus;
import fr.eiffelbikecorp.bikeapi.domain.enums.PurchaseStatus;
import fr.eiffelbikecorp.bikeapi.domain.enums.SaleOfferStatus;
import fr.eiffelbikecorp.bikeapi.dto.response.PurchaseResponse;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import fr.eiffelbikecorp.bikeapi.mapper.PurchaseMapper;
import fr.eiffelbikecorp.bikeapi.persistence.BasketRepository;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.PurchaseRepository;
import fr.eiffelbikecorp.bikeapi.persistence.SaleOfferRepository;
import fr.eiffelbikecorp.bikeapi.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private final CustomerRepository customerRepository;
    private final BasketRepository basketRepository;
    private final SaleOfferRepository saleOfferRepository;
    private final PurchaseRepository purchaseRepository;

    @Override
    @Transactional
    public PurchaseResponse checkout(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));
        Basket basket = basketRepository.findByCustomer_IdAndStatus(customerId, BasketStatus.OPEN)
                .orElseThrow(() -> new NotFoundException("Open basket not found for customer: " + customerId));
        if (basket.getItems().isEmpty()) {
            throw new BusinessRuleException("Basket is empty.");
        }
        List<SaleOffer> lockedOffers = basket.getItems().stream()
                .map(i -> saleOfferRepository.findByIdForUpdate(i.getOffer().getId())
                        .orElseThrow(() -> new NotFoundException("SaleOffer not found: " + i.getOffer().getId())))
                .toList();
        for (SaleOffer o : lockedOffers) {
            if (o.getStatus() != SaleOfferStatus.LISTED) {
                throw new BusinessRuleException("Offer is no longer available: " + o.getId());
            }
        }
        BigDecimal total = basket.getItems().stream()
                .map(BasketItem::getUnitPriceEurSnapshot)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime now = LocalDateTime.now();
        Purchase purchase = Purchase.builder()
                .customer(customer)
                .status(PurchaseStatus.CREATED)
                .totalAmountEur(total)
                .createdAt(now)
                .paidAt(null)
                .build();
        for (BasketItem bi : basket.getItems()) {
            SaleOffer offer = saleOfferRepository.getReferenceById(bi.getOffer().getId());
            PurchaseItem pi = PurchaseItem.builder()
                    .purchase(purchase)
                    .offer(offer)
                    .unitPriceEurSnapshot(bi.getUnitPriceEurSnapshot())
                    .build();
            purchase.getItems().add(pi);
        }
        Purchase saved = purchaseRepository.save(purchase);
        basket.setStatus(BasketStatus.CHECKED_OUT);
        basket.setUpdatedAt(now);
        basketRepository.save(basket);
        saved.getItems().size();
        return PurchaseMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseResponse getPurchase(UUID customerId, Long purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new NotFoundException("Purchase not found: " + purchaseId));
        if (!purchase.getCustomer().getId().equals(customerId)) {
            throw new BusinessRuleException("Purchase does not belong to customer.");
        }
        purchase.getItems().size();
        return PurchaseMapper.toResponse(purchase);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseResponse> listPurchases(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new NotFoundException("Customer not found: " + customerId);
        }
        return purchaseRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId).stream()
                .map(PurchaseMapper::toResponse)
                .toList();
    }
}
