package fr.eiffelbikecorp.bikeapi.configuration;

import fr.eiffelbikecorp.bikeapi.mapper.*;
import fr.eiffelbikecorp.bikeapi.security.AuthFilter;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.validation.ValidationFeature;
import org.springframework.context.annotation.Configuration;

@Configuration
@ApplicationPath("/api")
public class JerseyConfiguration extends ResourceConfig {
    @PostConstruct
    public void init() {
        // Enable Bean Validation (Jakarta Validation)
        register(ValidationFeature.class);
        // Controllers

        packages("fr.eiffelbikecorp.bikeapi.controller"); // JAX-RS resources
        // Exception mappers
        register(NotFoundExceptionMapper.class);
        register(BusinessRuleExceptionMapper.class);
        register(BadRequestExceptionMapper.class);
        register(AuthenticationExceptionMapper.class);
        register(GenericExceptionMapper.class);
        // filter
        register(AuthFilter.class);
        register(CorsFilter.class);
    }
}
