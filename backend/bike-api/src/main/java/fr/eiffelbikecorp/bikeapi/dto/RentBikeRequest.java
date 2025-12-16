package fr.eiffelbikecorp.bikeapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RentBikeRequest(
        @NotNull Long bikeId,
        @NotNull Long customerId,
        @NotNull @Min(1) Integer days
) {
}
