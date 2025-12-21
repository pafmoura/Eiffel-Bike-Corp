package fr.eiffelbikecorp.bikeapi.dto.response;

import java.util.UUID;

public record UserResponse(
        UUID customerId,
        String type,
        String fullName,
        String email,
        UUID providerId // null if plain CUSTOMER
) {}
