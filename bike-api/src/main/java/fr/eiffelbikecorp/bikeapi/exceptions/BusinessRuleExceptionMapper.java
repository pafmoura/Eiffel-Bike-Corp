package fr.eiffelbikecorp.bikeapi.exceptions;

import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.OffsetDateTime;
import java.util.List;

@Provider
public class BusinessRuleExceptionMapper implements ExceptionMapper<BusinessRuleException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(BusinessRuleException ex) {
        ApiError body = new ApiError(
                409,
                "Conflict",
                ex.getMessage(),
                uriInfo != null ? uriInfo.getPath() : null,
                OffsetDateTime.now(),
                List.of()
        );
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
