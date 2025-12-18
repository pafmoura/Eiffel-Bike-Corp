package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.SaleOffer;
import fr.eiffelbikecorp.bikeapi.domain.SaleOfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaleOfferRepository extends JpaRepository<SaleOffer, Long> {

    // US_11 search bikes to buy
    List<SaleOffer> findByStatusOrderByListedAtDesc(SaleOfferStatus status);

    // filter by keyword via bike description (simple but effective)
    List<SaleOffer> findByStatusAndBike_DescriptionContainingIgnoreCaseOrderByListedAtDesc(
            SaleOfferStatus status, String q
    );

    // one offer per bike
    Optional<SaleOffer> findByBike_Id(Long bikeId);

    // seller listings
    List<SaleOffer> findBySeller_IdOrderByListedAtDesc(UUID sellerId);

    // buyer purchases
    List<SaleOffer> findByBuyer_IdOrderBySoldAtDesc(UUID buyerCustomerId);
}
