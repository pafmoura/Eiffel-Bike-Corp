package fr.eiffelbikecorp.bikeapi.mapper;

import fr.eiffelbikecorp.bikeapi.domain.entity.Rental;
import fr.eiffelbikecorp.bikeapi.dto.response.RentalResponse;

public final class RentalMapper {
    private RentalMapper() {
    }

    public static RentalResponse toResponse(Rental r) {
        if (r == null) return null;
        return new RentalResponse(
                r.getId(),
                r.getBike().getId(),
                r.getCustomer().getId(),
                r.getStatus().name(),
                r.getStartAt(),
                r.getEndAt(),
                r.getTotalAmountEur()
        );
    }
}
