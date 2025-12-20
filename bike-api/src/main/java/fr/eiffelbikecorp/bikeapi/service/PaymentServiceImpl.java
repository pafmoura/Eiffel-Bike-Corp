package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.domain.*;
import fr.eiffelbikecorp.bikeapi.dto.PayPurchaseRequest;
import fr.eiffelbikecorp.bikeapi.dto.PayRentalRequest;
import fr.eiffelbikecorp.bikeapi.dto.RentalPaymentResponse;
import fr.eiffelbikecorp.bikeapi.dto.SalePaymentResponse;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import fr.eiffelbikecorp.bikeapi.mapper.RentalPaymentMapper;
import fr.eiffelbikecorp.bikeapi.mapper.SalePaymentMapper;
import fr.eiffelbikecorp.bikeapi.payment.PaymentGateway;
import fr.eiffelbikecorp.bikeapi.persistence.*;
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
            throw new BusinessRuleException("Only ACTIVE rentals can be paid." );
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
        /** TODO: gateway integration disabled fow now
         // 1) authorize funds (hold)
         var auth = paymentGateway.authorize(currency, request.amount(), request.paymentMethodId(), "rental:" + rental.getId());
         if (auth.status() != PaymentGateway.GatewayStatus.AUTHORIZED) {
         throw new BusinessRuleException("Payment not authorized: " + auth.message());
         }
         // 2) capture funds
         var capture = paymentGateway.capture(auth.authorizationId());
         if (capture.status() != PaymentGateway.GatewayStatus.PAID) {
         throw new BusinessRuleException("Payment capture failed: " + capture.message());
         }
         logger.info("Payment captured: " + capture.paymentId() + " for rental " + rental.getId());
         */
        // save payment record
        RentalPayment saved = rentalPaymentRepository.save(payment);
        return RentalPaymentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalPaymentResponse> listPayments(Long rentalId) {
        // gives clean 404 if rental doesn't exist
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
            throw new BusinessRuleException("Purchase does not belong to customer." );
        }
        if (purchase.getStatus() != PurchaseStatus.CREATED) {
            throw new BusinessRuleException("Only CREATED purchases can be paid." );
        }
        String currency = request.currency().trim().toUpperCase();
        BigDecimal rateToEur = fxRateService.getRateToEur(currency);
        BigDecimal amountEur = request.amount()
                .multiply(rateToEur)
                .setScale(2, RoundingMode.HALF_UP);
        // ensure enough amount to cover total (simple rule)
        if (amountEur.compareTo(purchase.getTotalAmountEur()) < 0) {
            throw new BusinessRuleException("Insufficient amount to cover purchase total in EUR." );
        }
        // Stripe: authorize then capture
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
                .stripePaymentIntentId(cap.paymentId()) // store PaymentIntent id
                .build();
        SalePayment saved = salePaymentRepository.save(payment);
        // mark purchase PAID
        purchase.setStatus(PurchaseStatus.PAID);
        purchase.setPaidAt(now);
        // mark offers SOLD (lock each one)
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
        // save purchase (offers will be saved by dirty checking)
        purchaseRepository.save(purchase);
        return SalePaymentMapper.toResponse(saved);
    }
}
