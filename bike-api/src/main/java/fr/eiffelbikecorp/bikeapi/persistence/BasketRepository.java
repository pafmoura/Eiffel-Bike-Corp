package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.entity.Basket;
import fr.eiffelbikecorp.bikeapi.domain.enums.BasketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BasketRepository extends JpaRepository<Basket, Long> {

    Optional<Basket> findByCustomer_IdAndStatus(UUID customerId, BasketStatus status);

    boolean existsByCustomer_IdAndStatus(UUID customerId, BasketStatus status);
}
