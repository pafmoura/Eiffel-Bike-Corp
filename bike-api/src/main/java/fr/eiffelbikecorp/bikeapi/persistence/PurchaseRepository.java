package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.Purchase;
import fr.eiffelbikecorp.bikeapi.domain.PurchaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);

    List<Purchase> findByCustomer_IdAndStatusOrderByCreatedAtDesc(UUID customerId, PurchaseStatus status);
}
