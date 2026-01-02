package fr.eiffelbikecorp.bikeapi;

import fr.eiffelbikecorp.bikeapi.configuration.ExchangeRateApiProperties;
import fr.eiffelbikecorp.bikeapi.configuration.StripeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties({ExchangeRateApiProperties.class, StripeProperties.class})
@EnableAsync
public class BikeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BikeApiApplication.class, args);
    }
}
