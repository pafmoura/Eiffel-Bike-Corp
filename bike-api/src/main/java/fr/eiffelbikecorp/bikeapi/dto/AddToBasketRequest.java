package fr.eiffelbikecorp.bikeapi.dto;

import jakarta.validation.constraints.NotNull;

public record AddToBasketRequest(
        @NotNull Long saleOfferId
) {}
