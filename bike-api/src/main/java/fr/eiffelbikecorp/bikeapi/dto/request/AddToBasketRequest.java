package fr.eiffelbikecorp.bikeapi.dto.request;

import jakarta.validation.constraints.NotNull;

public record AddToBasketRequest(
        @NotNull Long saleOfferId
) {}
