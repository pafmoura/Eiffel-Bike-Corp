package fr.eiffelbikecorp.bikeapi.dto;

import jakarta.validation.constraints.*;

public record CreateSaleNoteRequest(
        @NotNull Long saleOfferId,

        @NotBlank @Size(max = 255)
        String title,

        @NotBlank @Size(max = 2000)
        String content,

        @Size(max = 255)
        String createdBy
) {}
