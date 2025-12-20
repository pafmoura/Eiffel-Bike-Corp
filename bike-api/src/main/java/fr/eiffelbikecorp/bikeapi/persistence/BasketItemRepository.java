package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.BasketItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BasketItemRepository extends JpaRepository<BasketItem, Long> {

    List<BasketItem> findByBasket_Id(Long basketId);

    Optional<BasketItem> findByBasket_IdAndOffer_Id(Long basketId, Long saleOfferId);

    boolean existsByBasket_IdAndOffer_Id(Long basketId, Long saleOfferId);

    long deleteByBasket_IdAndOffer_Id(Long basketId, Long saleOfferId);
}
