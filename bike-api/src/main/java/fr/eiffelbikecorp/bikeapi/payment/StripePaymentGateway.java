package fr.eiffelbikecorp.bikeapi.payment;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentCreateParams;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static fr.eiffelbikecorp.bikeapi.payment.PaymentGateway.GatewayStatus.*;

@Service
@RequiredArgsConstructor
public class StripePaymentGateway implements PaymentGateway {

    @Override
    public AuthorizationResult authorize(String currency, BigDecimal amount, String paymentMethodId, String reference) {
        try {
            long minorUnits = toMinorUnits(amount);
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(minorUnits)
                    .setCurrency(currency.toLowerCase())
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                    .setPaymentMethod(paymentMethodId)
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    )
                    .build();
            PaymentIntent pi = PaymentIntent.create(params);
            // Stripe confirm can result in requires_action (3DS) or requires_capture (authorized). :contentReference[oaicite:2]{index=2}
            return switch (pi.getStatus()) {
                case "requires_capture" -> new AuthorizationResult(pi.getId(), AUTHORIZED, "Funds authorized.");
                case "requires_action" ->
                        new AuthorizationResult(pi.getId(), REQUIRES_ACTION, "3DS authentication required.");
                default -> new AuthorizationResult(pi.getId(), FAILED, "Authorization failed: " + pi.getStatus());
            };
        } catch (StripeException e) {
            throw new BusinessRuleException("Stripe authorization error: " + e.getMessage());
        }
    }

    @Override
    public CaptureResult capture(String authorizationId) {
        try {
            PaymentIntent pi = PaymentIntent.retrieve(authorizationId);
            if (!"requires_capture".equals(pi.getStatus())) {
                return new CaptureResult(pi.getId(), FAILED, "Not capturable: " + pi.getStatus());
            }
            PaymentIntent captured = pi.capture(PaymentIntentCaptureParams.builder().build());
            // Capture succeeds when status becomes succeeded. :contentReference[oaicite:3]{index=3}
            return "succeeded".equals(captured.getStatus())
                    ? new CaptureResult(captured.getId(), PAID, "Payment captured.")
                    : new CaptureResult(captured.getId(), FAILED, "Capture failed: " + captured.getStatus());
        } catch (StripeException e) {
            throw new BusinessRuleException("Stripe capture error: " + e.getMessage());
        }
    }

    private long toMinorUnits(BigDecimal amount) {
        // For EUR-like currencies: 2 decimals
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        return scaled.movePointRight(2).longValueExact();
    }
}
