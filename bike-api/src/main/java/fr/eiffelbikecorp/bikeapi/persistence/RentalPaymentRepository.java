package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.entity.RentalPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RentalPaymentRepository extends JpaRepository<RentalPayment, Long> {

    List<RentalPayment> findByRental_IdOrderByPaidAtDesc(Long rentalId);
}
