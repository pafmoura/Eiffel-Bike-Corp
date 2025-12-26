package fr.eiffelbikecorp.bikeapi.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReturnNoteResponse(
        Long id,
        String condition,
        String comment,
        LocalDateTime createdAt,
        UUID authorId
) {
}