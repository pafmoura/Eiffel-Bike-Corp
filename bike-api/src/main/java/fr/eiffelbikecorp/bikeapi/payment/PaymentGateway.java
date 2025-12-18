package fr.eiffelbikecorp.bikeapi.payment;

import java.math.BigDecimal;

public interface PaymentGateway {
    AuthorizationResult authorize(String currency, BigDecimal amount, String paymentMethodId, String reference);

    CaptureResult capture(String authorizationId);

    record AuthorizationResult(String authorizationId, GatewayStatus status, String message) {
    }

    record CaptureResult(String paymentId, GatewayStatus status, String message) {
    }

    enum GatewayStatus {AUTHORIZED, REQUIRES_ACTION, FAILED, PAID}
}
