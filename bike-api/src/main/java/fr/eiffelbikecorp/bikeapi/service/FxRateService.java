package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.configuration.ExchangeRateApiProperties;
import fr.eiffelbikecorp.bikeapi.dto.ExchangeRateApiResponse;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.Logger;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * And here's an example request: https://v6.exchangerate-api.com/v6/12a929e3195eb9152e5e1976/latest/USD
 *
 * @see https://www.exchangerate-api.com/docs/overview
 */
@Service
@RequiredArgsConstructor
public class FxRateService {

    Logger logger = Logger.getLogger(FxRateService.class);

    private final ExchangeRateApiProperties props;

    // simple in-memory cache (e.g., 30 minutes)
    private final AtomicReference<CachedRates> cache = new AtomicReference<>();

    public BigDecimal getRateToEur(String currency) {
        logger.info("Fetching FX rate to EUR for currency: " + currency);
        String c = currency.trim().toUpperCase();
        if ("EUR".equals(c)) return BigDecimal.ONE;
        Map<String, BigDecimal> eurToX = getEurBaseRates();
        BigDecimal eurToCurrency = eurToX.get(c);
        if (eurToCurrency == null) {
            throw new BusinessRuleException("Unsupported currency: " + c);
        }
        if (eurToCurrency.signum() <= 0) {
            throw new BusinessRuleException("Invalid FX rate for " + c);
        }
        // API gives: 1 EUR = eurToCurrency * C
        // We need: 1 C = rateToEur * EUR => rateToEur = 1 / eurToCurrency
        return BigDecimal.ONE.divide(eurToCurrency, 10, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> getEurBaseRates() {
        CachedRates existing = cache.get();
        if (existing != null && !existing.isExpired()) {
            return existing.rates();
        }
        RestClient client = RestClient.create();
        ExchangeRateApiResponse response = client.get()
                .uri(props.latestBaseUrl())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new BusinessRuleException("FX provider error: " + res.getStatusCode());
                })
                .body(ExchangeRateApiResponse.class);
        if (response == null || response.conversionRates() == null || !"success".equalsIgnoreCase(response.result())) {
            throw new BusinessRuleException("FX provider returned an invalid response.");
        }
        cache.set(new CachedRates(response.conversionRates(), Instant.now().plus(Duration.ofMinutes(30))));
        return response.conversionRates();
    }

    private record CachedRates(Map<String, BigDecimal> rates, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
