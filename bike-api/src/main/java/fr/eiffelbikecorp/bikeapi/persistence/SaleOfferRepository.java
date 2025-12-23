package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.entity.SaleOffer;
import fr.eiffelbikecorp.bikeapi.domain.enums.SaleOfferStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SaleOfferRepository extends JpaRepository<SaleOffer, Long> {

    List<SaleOffer> findByStatusOrderByListedAtDesc(SaleOfferStatus status);

    List<SaleOffer> findByStatusAndBike_DescriptionContainingIgnoreCaseOrderByListedAtDesc(
            SaleOfferStatus status, String q
    );

    Optional<SaleOffer> findByBike_Id(Long bikeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from SaleOffer o where o.id = :offerId")
    Optional<SaleOffer> findByIdForUpdate(@Param("offerId") Long offerId);
}
