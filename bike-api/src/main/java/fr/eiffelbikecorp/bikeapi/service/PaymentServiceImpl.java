package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.domain.PaymentStatus;
import fr.eiffelbikecorp.bikeapi.domain.Rental;
import fr.eiffelbikecorp.bikeapi.domain.RentalPayment;
import fr.eiffelbikecorp.bikeapi.domain.RentalStatus;
import fr.eiffelbikecorp.bikeapi.dto.PayRentalRequest;
import fr.eiffelbikecorp.bikeapi.dto.RentalPaymentResponse;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import fr.eiffelbikecorp.bikeapi.mapper.RentalPaymentMapper;
import fr.eiffelbikecorp.bikeapi.payment.PaymentGateway;
import fr.eiffelbikecorp.bikeapi.persistence.RentalPaymentRepository;
import fr.eiffelbikecorp.bikeapi.persistence.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    Logger logger = Logger.getLogger(PaymentServiceImpl.class);

    private final RentalRepository rentalRepository;
    private final RentalPaymentRepository rentalPaymentRepository;
    private final FxRateService fxRateService;
    private final PaymentGateway paymentGateway;

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
}
