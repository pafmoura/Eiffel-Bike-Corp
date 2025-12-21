package fr.eiffelbikecorp.bikeapi.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RentBikeRequest(
        @NotNull Long bikeId,
        @NotNull UUID customerId,
        @NotNull @Min(1) Integer days
) {
}
