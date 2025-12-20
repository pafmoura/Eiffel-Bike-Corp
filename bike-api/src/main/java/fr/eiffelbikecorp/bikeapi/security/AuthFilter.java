package fr.eiffelbikecorp.bikeapi.security;

import fr.eiffelbikecorp.bikeapi.dto.ApiError;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.Provider;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.Logger;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@Provider
@Secured
@Priority(Priorities.AUTHENTICATION)
@RequiredArgsConstructor
public class AuthFilter implements ContainerRequestFilter {

    Logger logger = Logger.getLogger(AuthFilter.class);

    private final CustomerRepository customerRepository;

    @Context
    private UriInfo uriInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
            logger.info("Bypassing authentication for OPTIONS request to " + requestContext.getUriInfo().getPath());
            return;
        }

        logger.info("Authenticating request to " + requestContext.getUriInfo().getPath());
        String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth == null || auth.isBlank() || !auth.startsWith("Bearer ")) {
            abort401(requestContext, "Missing or invalid Authorization header.");
            return;
        }
        String token = auth.substring("Bearer ".length()).trim();
        UUID userId;
        try {
            userId = UUID.fromString(token);
        } catch (IllegalArgumentException ex) {
            abort401(requestContext, "Invalid token.");
            return;
        }
        // check if the user exists. We don't check roles/permissions here for simplicity.
        // we just check the customer existence because all users are customers in this simplified model.
        if (!customerRepository.existsById(userId)) {
            abort404(requestContext, "User not found.");
            return;
        }
        // put the userId in the request context for further use in the resource methods
        requestContext.setProperty("userId", userId);
        logger.info("Authenticated user " + userId + " for request to " + requestContext.getUriInfo().getPath());
    }

    private void abort401(ContainerRequestContext ctx, String message) {
        ApiError body = new ApiError(
                401,
                "Unauthorized",
                message,
                uriInfo != null ? uriInfo.getPath() : null,
                OffsetDateTime.now(),
                List.of()
        );
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build());
    }

    private void abort404(ContainerRequestContext ctx, String message) {
        ApiError body = new ApiError(
                404,
                "Not Found",
                message,
                uriInfo != null ? uriInfo.getPath() : null,
                OffsetDateTime.now(),
                List.of()
        );
        ctx.abortWith(Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build());
    }
}
