package fr.eiffelbikecorp.bikeapi.security;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String type) {
}

