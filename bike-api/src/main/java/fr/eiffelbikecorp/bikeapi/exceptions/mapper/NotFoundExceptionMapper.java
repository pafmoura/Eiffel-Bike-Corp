package fr.eiffelbikecorp.bikeapi.exceptions.mapper;

import fr.eiffelbikecorp.bikeapi.dto.ApiError;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.OffsetDateTime;
import java.util.List;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(NotFoundException ex) {
        ApiError body = new ApiError(
                404,
                "Not Found",
                ex.getMessage(),
                uriInfo != null ? uriInfo.getPath() : null,
                OffsetDateTime.now(),
                List.of()
        );
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
