package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.entity.Rental;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    boolean existsByBike_IdAndStatus(Long bikeId, RentalStatus status);

    boolean existsByBike_Id(Long bikeId);
}
