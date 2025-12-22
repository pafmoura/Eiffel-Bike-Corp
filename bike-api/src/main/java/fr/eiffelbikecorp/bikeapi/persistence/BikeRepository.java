package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.entity.Bike;
import fr.eiffelbikecorp.bikeapi.domain.enums.BikeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BikeRepository extends JpaRepository<Bike, Long> {

    List<Bike> findByStatus(BikeStatus status);

    List<Bike> findByDescriptionContainingIgnoreCase(String q);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bike b where b.id = :id")
    Optional<Bike> findByIdForUpdate(@Param("id") Long id);

    List<Bike> findByStatusAndDescriptionContainingIgnoreCase(BikeStatus status, String q);
}
