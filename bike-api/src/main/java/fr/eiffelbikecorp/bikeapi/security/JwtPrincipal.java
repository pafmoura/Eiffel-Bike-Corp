package fr.eiffelbikecorp.bikeapi.security;

import java.security.Principal;
import java.util.UUID;

public class JwtPrincipal implements Principal {
    private final UUID userId;
    private final String type;

    public JwtPrincipal(UUID userId, String type) {
        this.userId = userId;
        this.type = type;
    }

    @Override
    public String getName() {
        return userId.toString();
    }

    public UUID userId() {
        return userId;
    }

    public String type() {
        return type;
    }
}