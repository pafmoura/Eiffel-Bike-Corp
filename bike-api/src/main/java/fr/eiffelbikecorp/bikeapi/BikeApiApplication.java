package fr.eiffelbikecorp.bikeapi;

import fr.eiffelbikecorp.bikeapi.configuration.ExchangeRateApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ExchangeRateApiProperties.class)
public class BikeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BikeApiApplication.class, args);
    }
}
