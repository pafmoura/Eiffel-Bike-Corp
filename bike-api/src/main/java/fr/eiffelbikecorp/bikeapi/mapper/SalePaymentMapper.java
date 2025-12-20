package fr.eiffelbikecorp.bikeapi.mapper;

import fr.eiffelbikecorp.bikeapi.domain.SalePayment;
import fr.eiffelbikecorp.bikeapi.dto.SalePaymentResponse;

public final class SalePaymentMapper {
    private SalePaymentMapper() {}

    public static SalePaymentResponse toResponse(SalePayment p) {
        return new SalePaymentResponse(
                p.getId(),
                p.getPurchase().getId(),
                p.getOriginalAmount(),
                p.getOriginalCurrency(),
                p.getFxRateToEur(),
                p.getAmountEur(),
                p.getStatus().name(),
                p.getPaidAt(),
                p.getStripePaymentIntentId()
        );
    }
}
