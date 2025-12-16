package fr.eiffelbikecorp.bikeapi.configuration;

import fr.eiffelbikecorp.bikeapi.controller.BikeCatalogController;
import fr.eiffelbikecorp.bikeapi.controller.PaymentController;
import fr.eiffelbikecorp.bikeapi.controller.RentalController;
import fr.eiffelbikecorp.bikeapi.controller.SaleController;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
@ApplicationPath("/api")
public class JerseyConfiguration extends ResourceConfig {
    @PostConstruct
    public void init() {
        // Controllers
        register(BikeCatalogController.class);
        register(RentalController.class);
        register(PaymentController.class);
        register(SaleController.class);
    }
}
