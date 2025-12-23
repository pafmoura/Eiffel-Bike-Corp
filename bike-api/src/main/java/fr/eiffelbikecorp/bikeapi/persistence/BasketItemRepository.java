package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.entity.BasketItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BasketItemRepository extends JpaRepository<BasketItem, Long> {

    boolean existsByBasket_IdAndOffer_Id(Long basketId, Long saleOfferId);

    long deleteByBasket_IdAndOffer_Id(Long basketId, Long saleOfferId);
}
