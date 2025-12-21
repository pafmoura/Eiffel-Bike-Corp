package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.entity.ReturnNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReturnNoteRepository extends JpaRepository<ReturnNote, Long> {

    // US_08 add notes on return (and later view)
    Optional<ReturnNote> findByRental_Id(Long rentalId);
    boolean existsByRental_Id(Long rentalId);
}
