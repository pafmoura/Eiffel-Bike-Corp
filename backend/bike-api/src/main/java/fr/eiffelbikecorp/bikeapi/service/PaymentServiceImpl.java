package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.domain.*;
import fr.eiffelbikecorp.bikeapi.dto.PayRentalRequest;
import fr.eiffelbikecorp.bikeapi.dto.RentalPaymentResponse;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import fr.eiffelbikecorp.bikeapi.mapper.RentalPaymentMapper;
import fr.eiffelbikecorp.bikeapi.persistence.RentalPaymentRepository;
import fr.eiffelbikecorp.bikeapi.persistence.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final RentalRepository rentalRepository;
    private final RentalPaymentRepository rentalPaymentRepository;
    private final FxRateService fxRateService;

    @Override
    @Transactional
    public RentalPaymentResponse payRental(PayRentalRequest request) {

        Rental rental = rentalRepository.findById(request.rentalId())
                .orElseThrow(() -> new NotFoundException("Rental not found: " + request.rentalId()));

        // You can decide if CLOSED rentals can still be paid; here we block it.
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
