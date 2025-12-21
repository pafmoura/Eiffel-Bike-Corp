package fr.eiffelbikecorp.bikeapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ReturnBikeRequest(
        @NotNull UUID authorCustomerId,
        @NotBlank @Size(max = 2000) String comment,
        @Size(max = 255) String condition
) {
}
