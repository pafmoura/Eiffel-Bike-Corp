package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.SalePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalePaymentRepository extends JpaRepository<SalePayment, Long> {

    List<SalePayment> findByPurchase_IdOrderByPaidAtDesc(Long purchaseId);

    Optional<SalePayment> findFirstByPurchase_IdOrderByPaidAtDesc(Long purchaseId);

    boolean existsByStripePaymentIntentId(String stripePaymentIntentId);
}
