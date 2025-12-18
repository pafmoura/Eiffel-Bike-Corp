package fr.eiffelbikecorp.bikeapi.exceptions;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.OffsetDateTime;
import java.util.List;

@Provider
public class BadRequestExceptionMapper implements ExceptionMapper<BadRequestException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(BadRequestException ex) {
        ApiError body = new ApiError(
                400,
                "Bad Request",
                safeMessage(ex),
                uriInfo != null ? uriInfo.getPath() : null,
                OffsetDateTime.now(),
                List.of()
        );
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }

    private String safeMessage(BadRequestException ex) {
        // Avoid leaking internal parsing details; keep it user-friendly.
        return "Invalid request payload or parameters.";
    }
}
