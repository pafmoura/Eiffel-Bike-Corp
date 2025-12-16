package fr.eiffelbikecorp.bikeapi.dto;

public record ProviderRef(
        String type, // "CUSTOMER" or "CORP"
        Long id
) {
}
