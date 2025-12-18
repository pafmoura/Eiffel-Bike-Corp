package fr.eiffelbikecorp.bikeapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserLoginRequest(
        @NotBlank @Email @Size(max = 255)
        String email,

        @NotBlank @Size(max = 255)
        String password
) {}