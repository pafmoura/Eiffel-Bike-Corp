package fr.eiffelbikecorp.bikeapi.security;

import fr.eiffelbikecorp.bikeapi.dto.ApiError;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

@Component
@Provider
@Secured
@Priority(Priorities.AUTHENTICATION)
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class AuthFilter implements ContainerRequestFilter {

    Logger logger = Logger.getLogger(AuthFilter.class);

    private final CustomerRepository customerRepository;

    private final TokenService tokenService;

    private final EiffelBikeCorpRepository eiffelBikeCorpRepository;

    @Context
    private UriInfo uriInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (requestContext.getMethod().equalsIgnoreCase("OPTIONS")) return;
        String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth == null || auth.isBlank() || !auth.startsWith("Bearer ")) {
            abort401(requestContext, "Missing or invalid Authorization header.");
            return;
        }
        String token = auth.substring("Bearer ".length()).trim();
        var user = tokenService.validateAndGetUser(token); // <-- (UUID + type)
        if (user == null || user.userId() == null) {
            abort401(requestContext, "Invalid or expired token.");
            return;
        }
        if (!customerRepository.existsById(user.userId())) {
            abort404(requestContext, "User not found.");
            return;
        }
        requestContext.setProperty("userId", user.userId());
        requestContext.setProperty("userType", user.type());
        requestContext.setSecurityContext(
                new JwtSecurityContext(new JwtPrincipal(user.userId(), user.type()),
                        requestContext.getSecurityContext().isSecure())
        );
        logger.info("Authenticated user " + user.userId() + " type=" + user.type());
    }

    private void abort401(ContainerRequestContext ctx, String message) {
        ApiError body = new ApiError(
                401, "Unauthorized", message,
                uriInfo != null ? uriInfo.getPath() : null,
                OffsetDateTime.now(), List.of()
        );
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON).entity(body).build());
    }

    private void abort404(ContainerRequestContext ctx, String message) {
        ApiError body = new ApiError(
                404, "Not Found", message,
                uriInfo != null ? uriInfo.getPath() : null,
                OffsetDateTime.now(), List.of()
        );
        ctx.abortWith(Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON).entity(body).build());
    }
}