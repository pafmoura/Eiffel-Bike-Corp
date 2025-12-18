package fr.eiffelbikecorp.bikeapi.dto;

import java.util.UUID;

public record ProviderRef(
        String type, // "CUSTOMER" or "CORP"
        UUID id
) {
}
