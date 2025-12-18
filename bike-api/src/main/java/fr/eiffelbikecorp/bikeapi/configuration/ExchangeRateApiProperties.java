package fr.eiffelbikecorp.bikeapi.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fx.exchangerate")
public record ExchangeRateApiProperties(
        String apiKey,
        String baseUrl,
        String baseCode
) {
    public String latestBaseUrl() {
        // example: https://v6.exchangerate-api.com/v6/<key>/latest/EUR
        return baseUrl + "/" + apiKey + "/latest/" + baseCode;
    }
}

