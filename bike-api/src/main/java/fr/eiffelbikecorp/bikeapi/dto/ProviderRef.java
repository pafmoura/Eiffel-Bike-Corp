package fr.eiffelbikecorp.bikeapi.dto;

import java.util.UUID;

public record ProviderRef(
        String type,
        UUID id
) {
}
