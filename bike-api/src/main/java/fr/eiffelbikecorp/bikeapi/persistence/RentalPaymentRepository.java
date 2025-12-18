package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.PaymentStatus;
import fr.eiffelbikecorp.bikeapi.domain.RentalPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RentalPaymentRepository extends JpaRepository<RentalPayment, Long> {

    // US_07 payments for a rental
    List<RentalPayment> findByRental_IdOrderByPaidAtDesc(Long rentalId);

    List<RentalPayment> findByRental_IdAndStatusOrderByPaidAtDesc(Long rentalId, PaymentStatus status);

    Optional<RentalPayment> findFirstByRental_IdAndStatusOrderByPaidAtDesc(Long rentalId, PaymentStatus status);
}
