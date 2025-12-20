package fr.eiffelbikecorp.bikeapi.mapper;

import fr.eiffelbikecorp.bikeapi.domain.Purchase;
import fr.eiffelbikecorp.bikeapi.domain.PurchaseItem;
import fr.eiffelbikecorp.bikeapi.dto.PurchaseItemResponse;
import fr.eiffelbikecorp.bikeapi.dto.PurchaseResponse;

import java.util.List;

public final class PurchaseMapper {
    private PurchaseMapper() {}

    public static PurchaseResponse toResponse(Purchase p) {
        List<PurchaseItemResponse> items = p.getItems().stream()
                .map(PurchaseMapper::toItemResponse)
                .toList();

        return new PurchaseResponse(
                p.getId(),
                p.getStatus().name(),
                p.getTotalAmountEur(),
                p.getCreatedAt(),
                p.getPaidAt(),
                items
        );
    }

    private static PurchaseItemResponse toItemResponse(PurchaseItem i) {
        return new PurchaseItemResponse(
                i.getId(),
                i.getOffer().getId(),
                i.getOffer().getBike().getId(),
                i.getUnitPriceEurSnapshot()
        );
    }
}
