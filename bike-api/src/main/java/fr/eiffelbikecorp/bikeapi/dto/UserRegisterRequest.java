package fr.eiffelbikecorp.bikeapi.dto;

import fr.eiffelbikecorp.bikeapi.domain.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
        @NotNull UserType type,

        @NotBlank @Size(max = 255)
        String fullName,

        @NotBlank @Email @Size(max = 255)
        String email
) {
}
