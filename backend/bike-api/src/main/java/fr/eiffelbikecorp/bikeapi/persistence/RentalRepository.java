package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.Rental;
import fr.eiffelbikecorp.bikeapi.domain.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    // customer rentals
    List<Rental> findByCustomer_IdOrderByStartAtDesc(Long customerId);

    // bike rentals
    List<Rental> findByBike_IdOrderByStartAtDesc(Long bikeId);

    // US_06 enforce one active rental per bike (service layer uses these)
    Optional<Rental> findFirstByBike_IdAndStatus(Long bikeId, RentalStatus status);
    boolean existsByBike_IdAndStatus(Long bikeId, RentalStatus status);

    // active rentals list
    List<Rental> findByStatus(RentalStatus status);
}
