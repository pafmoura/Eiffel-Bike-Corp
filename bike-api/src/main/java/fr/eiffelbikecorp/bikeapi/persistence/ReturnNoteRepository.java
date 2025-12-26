package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.entity.ReturnNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnNoteRepository extends JpaRepository<ReturnNote, Long> {
    boolean existsByRental_Id(Long rentalId);

    List<ReturnNote> findAllByRental_Bike_IdOrderByCreatedAtDesc(Long bikeId);
}
