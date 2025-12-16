package fr.eiffelbikecorp.bikeapi.service;

import java.math.BigDecimal;

public interface FxRateService {
    /**
     * @return rate such that: amountEur = originalAmount * rate
     * Example: USD->EUR might be 0.92
     */
    BigDecimal getRateToEur(String currency);
}
