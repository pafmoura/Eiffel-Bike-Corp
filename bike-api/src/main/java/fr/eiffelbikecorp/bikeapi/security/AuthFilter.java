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

    // 1. INJETAR O TOKEN SERVICE AQUI
    private final TokenService tokenService;

    @Context
    private UriInfo uriInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
            return;
        }

        String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth == null || auth.isBlank() || !auth.startsWith("Bearer ")) {
            abort401(requestContext, "Missing or invalid Authorization header.");
            return;
        }

        String token = auth.substring("Bearer ".length()).trim();

        // 2. CORREÇÃO: Usar o TokenService para validar o JWT e obter o UUID
        UUID userId = tokenService.validateAndGetUserId(token);

        // Se o token for inválido ou expirado, o método acima retorna null (conforme o teu TokenService)
        if (userId == null) {
            abort401(requestContext, "Invalid or expired token.");
            return;
        }

        // check if the user exists
        if (!customerRepository.existsById(userId)) {
            abort404(requestContext, "User not found.");
            return;
        }

        requestContext.setProperty("userId", userId);
        logger.info("Authenticated user " + userId);
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