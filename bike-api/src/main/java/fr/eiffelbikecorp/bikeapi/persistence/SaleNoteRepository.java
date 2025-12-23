package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.entity.SaleNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleNoteRepository extends JpaRepository<SaleNote, Long> {

    List<SaleNote> findBySaleOffer_IdOrderByCreatedAtDesc(Long saleOfferId);
}
