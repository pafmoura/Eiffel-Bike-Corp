package fr.univeiffel.bikerentalapi.config;

import org.glassfish.jersey.server.ResourceConfig;

public class AppConfig extends ResourceConfig {
    public AppConfig() {
        packages("fr.univeiffel.bikerentalapi");
    }
}