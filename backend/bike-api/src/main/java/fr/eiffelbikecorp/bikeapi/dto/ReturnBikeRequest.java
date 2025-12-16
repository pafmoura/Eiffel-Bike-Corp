package fr.eiffelbikecorp.bikeapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReturnBikeRequest(
        @NotNull Long authorCustomerId,
        @NotBlank @Size(max = 2000) String comment,
        @Size(max = 255) String condition
) {
}
