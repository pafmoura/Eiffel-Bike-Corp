package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.SaleNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleNoteRepository extends JpaRepository<SaleNote, Long> {

    // US_12 view notes associated with a sale offer / bike
    List<SaleNote> findBySaleOffer_IdOrderByCreatedAtDesc(Long saleOfferId);

    // convenience: notes by bike id (through saleOffer)
    List<SaleNote> findBySaleOffer_Bike_IdOrderByCreatedAtDesc(Long bikeId);
}
