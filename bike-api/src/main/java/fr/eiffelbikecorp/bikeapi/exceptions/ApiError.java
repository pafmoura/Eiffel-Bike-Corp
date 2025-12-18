package fr.eiffelbikecorp.bikeapi.exceptions;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
        int status,
        String error,
        String message,
        String path,
        OffsetDateTime timestamp,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }
}
