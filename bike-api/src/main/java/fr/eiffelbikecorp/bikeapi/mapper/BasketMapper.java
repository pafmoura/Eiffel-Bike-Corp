package fr.eiffelbikecorp.bikeapi.mapper;

import fr.eiffelbikecorp.bikeapi.domain.entity.Basket;
import fr.eiffelbikecorp.bikeapi.domain.entity.BasketItem;
import fr.eiffelbikecorp.bikeapi.dto.response.BasketItemResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.BasketResponse;

import java.util.List;

public final class BasketMapper {
    private BasketMapper() {}

    public static BasketResponse toResponse(Basket b) {
        List<BasketItemResponse> items = b.getItems().stream()
                .map(BasketMapper::toItemResponse)
                .toList();

        return new BasketResponse(
                b.getId(),
                b.getStatus().name(),
                b.getCreatedAt(),
                b.getUpdatedAt(),
                items
        );
    }

    private static BasketItemResponse toItemResponse(BasketItem i) {
        return new BasketItemResponse(
                i.getId(),
                i.getOffer().getId(),
                i.getOffer().getBike().getId(),
                i.getUnitPriceEurSnapshot(),
                i.getAddedAt()
        );
    }
}
