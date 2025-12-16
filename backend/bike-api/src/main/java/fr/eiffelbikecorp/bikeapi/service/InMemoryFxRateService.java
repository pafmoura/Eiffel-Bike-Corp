package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class InMemoryFxRateService implements FxRateService {

    private final Map<String, BigDecimal> ratesToEur = Map.of(
            "EUR", BigDecimal.ONE,
            "USD", new BigDecimal("0.92"),
            "BRL", new BigDecimal("0.18"),
            "GBP", new BigDecimal("1.16")
    );

    @Override
    public BigDecimal getRateToEur(String currency) {
        String c = currency.trim().toUpperCase();
        BigDecimal rate = ratesToEur.get(c);
        if (rate == null) {
            throw new BusinessRuleException("Unsupported currency: " + c);
        }
        return rate;
    }
}
