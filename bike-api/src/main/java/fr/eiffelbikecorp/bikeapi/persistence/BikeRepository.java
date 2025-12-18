package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.Bike;
import fr.eiffelbikecorp.bikeapi.domain.BikeStatus;
import fr.eiffelbikecorp.bikeapi.domain.BikeProvider;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BikeRepository extends JpaRepository<Bike, Long> {

    // US_05 search bikes to rent (basic)
    List<Bike> findByStatus(BikeStatus status);

    // useful for provider listings
    List<Bike> findByOfferedBy(BikeProvider offeredBy);
    List<Bike> findByOfferedBy_Id(UUID providerId);

    // small convenience for “search by text”
    List<Bike> findByDescriptionContainingIgnoreCase(String q);

    // critical for US_06 rent a bike (avoid concurrent rentals)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bike b where b.id = :id")
    Optional<Bike> findByIdForUpdate(@Param("id") Long id);

    // Optional “rent search”: available bikes matching query
    List<Bike> findByStatusAndDescriptionContainingIgnoreCase(BikeStatus status, String q);
}
