package fr.eiffelbikecorp.bikeapi.dto.response;

import java.util.UUID;

public record UserLoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {}
