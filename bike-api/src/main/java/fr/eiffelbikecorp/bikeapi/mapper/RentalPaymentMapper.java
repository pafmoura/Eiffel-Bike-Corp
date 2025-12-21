package fr.eiffelbikecorp.bikeapi.mapper;

import fr.eiffelbikecorp.bikeapi.domain.entity.RentalPayment;
import fr.eiffelbikecorp.bikeapi.dto.response.RentalPaymentResponse;

public final class RentalPaymentMapper {
    private RentalPaymentMapper() {}

    public static RentalPaymentResponse toResponse(RentalPayment p) {
        if (p == null) return null;
        return new RentalPaymentResponse(
                p.getId(),
                p.getRental().getId(),
                p.getOriginalAmount(),
                p.getOriginalCurrency(),
                p.getFxRateToEur(),
                p.getAmountEur(),
                p.getStatus().name(),
                p.getPaidAt()
        );
    }
}
