package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.SaleOffer;
import fr.eiffelbikecorp.bikeapi.domain.SaleOfferStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Offers considered "available for basket/purchase"
    @Query("""
        select o
        from SaleOffer o
        where o.status = 'LISTED'
        order by o.listedAt desc
    """)
    List<SaleOffer> findAvailableOffers();

    // Lock offer row during checkout/payment to avoid double-sell
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from SaleOffer o where o.id = :offerId")
    Optional<SaleOffer> findByIdForUpdate(@Param("offerId") Long offerId);
}
