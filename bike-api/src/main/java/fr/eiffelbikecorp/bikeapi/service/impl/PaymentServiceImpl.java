package fr.eiffelbikecorp.bikeapi.service.impl;

import fr.eiffelbikecorp.bikeapi.domain.entity.*;
import fr.eiffelbikecorp.bikeapi.domain.enums.PaymentStatus;
import fr.eiffelbikecorp.bikeapi.domain.enums.PurchaseStatus;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentalStatus;
import fr.eiffelbikecorp.bikeapi.domain.enums.SaleOfferStatus;
import fr.eiffelbikecorp.bikeapi.dto.request.PayPurchaseRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.PayRentalRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.RentalPaymentResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.SalePaymentResponse;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import fr.eiffelbikecorp.bikeapi.mapper.RentalPaymentMapper;
import fr.eiffelbikecorp.bikeapi.mapper.SalePaymentMapper;
import fr.eiffelbikecorp.bikeapi.payment.PaymentGateway;
import fr.eiffelbikecorp.bikeapi.persistence.*;
import fr.eiffelbikecorp.bikeapi.service.FxRateService;
import fr.eiffelbikecorp.bikeapi.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    Logger logger = Logger.getLogger(PaymentServiceImpl.class);

    private final RentalRepository rentalRepository;
    private final RentalPaymentRepository rentalPaymentRepository;
    private final FxRateService fxRateService;
    private final PaymentGateway paymentGateway;
    private final SaleOfferRepository saleOfferRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final CustomerRepository customerRepository;
    private final PurchaseRepository purchaseRepository;

    @Override
    @Transactional
    public RentalPaymentResponse payRental(PayRentalRequest request) {
        Rental rental = rentalRepository.findById(request.rentalId())
                .orElseThrow(() -> new NotFoundException("Rental not found: " + request.rentalId()));
        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new BusinessRuleException("Only ACTIVE rentals can be paid.");
        }
        String currency = request.currency().trim().toUpperCase();
        BigDecimal rateToEur = fxRateService.getRateToEur(currency);
        BigDecimal amountEur = request.amount()
                .multiply(rateToEur)
                .setScale(2, RoundingMode.HALF_UP);
        RentalPayment payment = RentalPayment.builder()
                .rental(rental)
                .originalAmount(request.amount())
                .originalCurrency(currency)
                .fxRateToEur(rateToEur)
                .amountEur(amountEur)
                .status(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();
        var auth = paymentGateway.authorize(currency, request.amount(), request.paymentMethodId(), "rental:" + rental.getId());
        if (auth.status() != PaymentGateway.GatewayStatus.AUTHORIZED) {
            throw new BusinessRuleException("Payment not authorized: " + auth.message());
        }
        var capture = paymentGateway.capture(auth.authorizationId());
        if (capture.status() != PaymentGateway.GatewayStatus.PAID) {
            throw new BusinessRuleException("Payment capture failed: " + capture.message());
        }
        logger.info("Payment captured: " + capture.paymentId() + " for rental " + rental.getId());
        RentalPayment saved = rentalPaymentRepository.save(payment);
        return RentalPaymentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalPaymentResponse> listPayments(Long rentalId) {
        if (!rentalRepository.existsById(rentalId)) {
            throw new NotFoundException("Rental not found: " + rentalId);
        }
        return rentalPaymentRepository.findByRental_IdOrderByPaidAtDesc(rentalId)
                .stream()
                .map(RentalPaymentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SalePaymentResponse payPurchase(UUID customerId, PayPurchaseRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));
        Purchase purchase = purchaseRepository.findById(request.purchaseId())
                .orElseThrow(() -> new NotFoundException("Purchase not found: " + request.purchaseId()));
        if (!purchase.getCustomer().getId().equals(customerId)) {
            throw new BusinessRuleException("Purchase does not belong to customer.");
        }
        if (purchase.getStatus() != PurchaseStatus.CREATED) {
            throw new BusinessRuleException("Only CREATED purchases can be paid.");
        }
        String currency = request.currency().trim().toUpperCase();
        BigDecimal rateToEur = fxRateService.getRateToEur(currency);
        BigDecimal amountEur = request.amount()
                .multiply(rateToEur)
                .setScale(2, RoundingMode.HALF_UP);
        if (amountEur.compareTo(purchase.getTotalAmountEur()) < 0) {
            throw new BusinessRuleException("Insufficient amount to cover purchase total in EUR.");
        }
        PaymentGateway.AuthorizationResult auth = paymentGateway.authorize(
                currency,
                request.amount(),
                request.paymentMethodId(),
                "purchase:" + purchase.getId()
        );
        if (auth.status() != PaymentGateway.GatewayStatus.AUTHORIZED) {
            throw new BusinessRuleException("Payment not authorized: " + auth.message());
        }
        PaymentGateway.CaptureResult cap = paymentGateway.capture(auth.authorizationId());
        if (cap.status() != PaymentGateway.GatewayStatus.PAID) {
            throw new BusinessRuleException("Payment capture failed: " + cap.message());
        }
        LocalDateTime now = LocalDateTime.now();
        SalePayment payment = SalePayment.builder()
                .purchase(purchase)
                .originalAmount(request.amount())
                .originalCurrency(currency)
                .fxRateToEur(rateToEur)
                .amountEur(amountEur)
                .status(PaymentStatus.PAID)
                .paidAt(now)
                .stripePaymentIntentId("some_id")
                .build();
        SalePayment saved = salePaymentRepository.save(payment);
        purchase.setStatus(PurchaseStatus.PAID);
        purchase.setPaidAt(now);
        purchase.getItems().forEach(it -> {
            SaleOffer offer = saleOfferRepository.findByIdForUpdate(it.getOffer().getId())
                    .orElseThrow(() -> new NotFoundException("SaleOffer not found: " + it.getOffer().getId()));
            if (offer.getStatus() != SaleOfferStatus.LISTED) {
                throw new BusinessRuleException("Offer is no longer available: " + offer.getId());
            }
            offer.setStatus(SaleOfferStatus.SOLD);
            offer.setBuyer(customer);
            offer.setSoldAt(now);
        });
        purchaseRepository.save(purchase);
        return SalePaymentMapper.toResponse(saved);
    }
}
