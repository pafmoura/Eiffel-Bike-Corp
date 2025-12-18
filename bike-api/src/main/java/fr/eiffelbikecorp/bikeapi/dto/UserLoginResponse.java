package fr.eiffelbikecorp.bikeapi.dto;

import java.util.UUID;

public record UserLoginResponse(
        UUID customerId,
        String token
) {}
