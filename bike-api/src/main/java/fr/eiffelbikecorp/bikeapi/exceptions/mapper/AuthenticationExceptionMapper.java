package fr.eiffelbikecorp.bikeapi.exceptions.mapper;

import fr.eiffelbikecorp.bikeapi.dto.ApiError;
import fr.eiffelbikecorp.bikeapi.exceptions.AuthenticationException;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.OffsetDateTime;
import java.util.List;

@Provider
public class AuthenticationExceptionMapper implements ExceptionMapper<AuthenticationException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(AuthenticationException ex) {
        ApiError body = new ApiError(
                401,
                "Unauthorized",
                ex.getMessage(),
                uriInfo != null ? uriInfo.getPath() : null,
                OffsetDateTime.now(),
                List.of()
        );

        return Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
