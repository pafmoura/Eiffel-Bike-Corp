package fr.eiffelbikecorp.bikeapi.configuration;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
        String secretKey,
        String currencyDefault
) {
    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }
}
