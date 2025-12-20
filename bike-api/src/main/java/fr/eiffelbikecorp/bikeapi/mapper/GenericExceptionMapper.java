package fr.eiffelbikecorp.bikeapi.mapper;

import fr.eiffelbikecorp.bikeapi.dto.ApiError;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.OffsetDateTime;
import java.util.List;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable ex) {
        ApiError body = new ApiError(
                500,
                "Internal Server Error",
                "An unexpected error occurred.",
                uriInfo != null ? uriInfo.getPath() : null,
                OffsetDateTime.now(),
                List.of()
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
