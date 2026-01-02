package fr.eiffelbikecorp.bikeapi.service.impl;

import fr.eiffelbikecorp.bikeapi.domain.entity.Customer;
import fr.eiffelbikecorp.bikeapi.domain.entity.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.domain.entity.Purchase;
import fr.eiffelbikecorp.bikeapi.domain.entity.SaleOffer;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.domain.enums.SaleOfferStatus;
import fr.eiffelbikecorp.bikeapi.dto.request.*;
import fr.eiffelbikecorp.bikeapi.dto.response.*;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import fr.eiffelbikecorp.bikeapi.payment.PaymentGateway;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
import fr.eiffelbikecorp.bikeapi.persistence.PurchaseRepository;
import fr.eiffelbikecorp.bikeapi.persistence.SaleOfferRepository;
import fr.eiffelbikecorp.bikeapi.service.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentServiceImplTest {

    @Autowired
    PaymentService paymentService;

    @Autowired
    BikeCatalogService bikeCatalogService;
    @Autowired
    RentalService rentalService;
    @Autowired
    SaleOfferService saleOfferService;
    @Autowired
    BasketService basketService;
    @Autowired
    PurchaseService purchaseService;

    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    EiffelBikeCorpRepository corpRepository;
    @Autowired
    PurchaseRepository purchaseRepository;
    @Autowired
    SaleOfferRepository saleOfferRepository;

    @MockitoBean(enforceOverride = true)
    PaymentGateway paymentGateway;
    @MockitoBean(enforceOverride = true)
    FxRateService fxRateService;

    private UUID corpId;
    private UUID customerId;
    private UUID otherCustomerId;

    @BeforeEach
    void setup() {
        EiffelBikeCorp corp = new EiffelBikeCorp();
        corpId = corpRepository.saveAndFlush(corp).getId();
        Customer c1 = new Customer();
        c1.setId(UUID.randomUUID());
        c1.setFullName("Customer 1");
        c1.setEmail("pay-" + UUID.randomUUID() + "@test.com");
        c1.setPassword("testpassword");
        customerId = customerRepository.saveAndFlush(c1).getId();
        Customer c2 = new Customer();
        c2.setId(UUID.randomUUID());
        c2.setFullName("Customer 2");
        c2.setEmail("pay-other-" + UUID.randomUUID() + "@test.com");
        c2.setPassword("testpassword");
        otherCustomerId = customerRepository.saveAndFlush(c2).getId();
    }

    @Test
    void should_pay_rental_in_foreign_currency_and_return_payment_response() {
        Long rentalId = createActiveRental(customerId);
        when(fxRateService.getRateToEur("USD")).thenReturn(new BigDecimal("0.90"));
        PaymentGateway.AuthorizationResult auth = mock(PaymentGateway.AuthorizationResult.class);
        when(auth.status()).thenReturn(PaymentGateway.GatewayStatus.AUTHORIZED);
        when(auth.authorizationId()).thenReturn("auth_rental_1");
        when(auth.message()).thenReturn("ok");
        PaymentGateway.CaptureResult cap = mock(PaymentGateway.CaptureResult.class);
        when(cap.status()).thenReturn(PaymentGateway.GatewayStatus.PAID);
        when(cap.paymentId()).thenReturn("pi_rental_1");
        when(cap.message()).thenReturn("ok");
        when(paymentGateway.authorize(eq("USD"), any(BigDecimal.class), eq("pm_card_visa"), startsWith("rental:")))
                .thenReturn(auth);
        when(paymentGateway.capture("auth_rental_1")).thenReturn(cap);
        PayRentalRequest payReq = new PayRentalRequest(
                rentalId,
                new BigDecimal("10.00"),
                "USD",
                "pm_card_visa"
        );
        // Act
        RentalPaymentResponse resp = paymentService.payRental(payReq);
        // Assert (RentalPaymentResponse record fields)
        assertThat(resp).isNotNull();
        assertThat(resp.id()).isNotNull();
        assertThat(resp.rentalId()).isEqualTo(rentalId);
        assertThat(resp.originalAmount()).isEqualByComparingTo("10.00");
        assertThat(resp.originalCurrency()).isEqualTo("USD");
        assertThat(resp.fxRateToEur()).isEqualByComparingTo("0.90");
        assertThat(resp.amountEur()).isEqualByComparingTo("9.00");
        assertThat(resp.status()).isEqualTo("PAID");
        assertThat(resp.paidAt()).isNotNull();
    }

    @Test
    void should_throw_NotFoundException_when_paying_unknown_rental() {
        when(fxRateService.getRateToEur("USD")).thenReturn(new BigDecimal("0.90"));
        assertThatThrownBy(() -> paymentService.payRental(new PayRentalRequest(
                999999L,
                new BigDecimal("10.00"),
                "USD",
                "pm_card_visa"
        )))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Rental not found");
    }

    @Test
    void should_throw_BusinessRuleException_when_rental_is_not_active() {
        // Arrange: create rental then close it via returnBike()
        Long rentalId = createActiveRental(customerId);
        rentalService.returnBike(rentalId, new ReturnBikeRequest(customerId, "returned", "GOOD"));
        // Act + Assert
        assertThatThrownBy(() -> paymentService.payRental(new PayRentalRequest(
                rentalId,
                new BigDecimal("10.00"),
                "USD",
                "pm_card_visa"
        )))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only ACTIVE rentals can be paid");
    }
    // -------------------------
    // payPurchase(UUID, PayPurchaseRequest)
    // -------------------------

    @Test
    void should_pay_purchase_and_mark_purchase_paid_and_offers_sold() {
        // Arrange: create purchase in CREATED
        PurchaseResponse purchase = createPurchaseCreated(customerId, new BigDecimal("199.99"));
        when(fxRateService.getRateToEur("USD")).thenReturn(new BigDecimal("1.00"));
        PaymentGateway.AuthorizationResult auth = mock(PaymentGateway.AuthorizationResult.class);
        when(auth.status()).thenReturn(PaymentGateway.GatewayStatus.AUTHORIZED);
        when(auth.authorizationId()).thenReturn("auth_purchase_1");
        when(auth.message()).thenReturn("ok");
        PaymentGateway.CaptureResult cap = mock(PaymentGateway.CaptureResult.class);
        when(cap.status()).thenReturn(PaymentGateway.GatewayStatus.PAID);
        when(cap.paymentId()).thenReturn("pi_purchase_1");
        when(cap.message()).thenReturn("ok");
        when(paymentGateway.authorize(eq("USD"), any(BigDecimal.class), eq("pm_card_visa"), startsWith("purchase:")))
                .thenReturn(auth);
        when(paymentGateway.capture("auth_purchase_1")).thenReturn(cap);
        PayPurchaseRequest req = new PayPurchaseRequest(
                purchase.id(),
                new BigDecimal("199.99"),
                "usd",
                "pm_card_visa"
        );
        // Act
        SalePaymentResponse resp = paymentService.payPurchase(customerId, req);
        // Assert response
        assertThat(resp).isNotNull();
        assertThat(resp.id()).isNotNull();
        assertThat(resp.purchaseId()).isEqualTo(purchase.id());
        assertThat(resp.originalAmount()).isEqualByComparingTo("199.99");
        assertThat(resp.originalCurrency()).isEqualTo("USD");
        assertThat(resp.fxRateToEur()).isEqualByComparingTo("1.00");
        assertThat(resp.amountEur()).isEqualByComparingTo("199.99");
        assertThat(resp.status()).isEqualTo("PAID");
        assertThat(resp.paidAt()).isNotNull();
        assertThat(resp.stripePaymentIntentId()).isEqualTo("some_id"); // as implemented in service
        // Assert DB side-effects
        Purchase savedPurchase = purchaseRepository.findById(purchase.id())
                .orElseThrow(() -> new AssertionError("Purchase should exist"));
        assertThat(savedPurchase.getStatus().name()).isEqualTo("PAID");
        assertThat(savedPurchase.getPaidAt()).isNotNull();
        Long offerId = purchase.items().get(0).saleOfferId();
        SaleOffer savedOffer = saleOfferRepository.findById(offerId)
                .orElseThrow(() -> new AssertionError("Offer should exist"));
        assertThat(savedOffer.getStatus()).isEqualTo(SaleOfferStatus.SOLD);
        assertThat(savedOffer.getBuyer()).isNotNull();
        assertThat(savedOffer.getBuyer().getId()).isEqualTo(customerId);
        assertThat(savedOffer.getSoldAt()).isNotNull();
    }

    @Test
    void should_throw_BusinessRuleException_when_purchase_amount_is_insufficient_in_eur() {
        PurchaseResponse purchase = createPurchaseCreated(customerId, new BigDecimal("200.00"));
        when(fxRateService.getRateToEur("EUR")).thenReturn(new BigDecimal("1.00"));
        assertThatThrownBy(() -> paymentService.payPurchase(customerId, new PayPurchaseRequest(
                purchase.id(),
                new BigDecimal("199.99"),
                "EUR",
                "pm_card_visa"
        )))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient amount");
    }

    @Test
    void should_throw_BusinessRuleException_when_purchase_does_not_belong_to_customer() {
        PurchaseResponse purchase = createPurchaseCreated(customerId, new BigDecimal("120.00"));
        when(fxRateService.getRateToEur("EUR")).thenReturn(new BigDecimal("1.00"));
        assertThatThrownBy(() -> paymentService.payPurchase(otherCustomerId, new PayPurchaseRequest(
                purchase.id(),
                new BigDecimal("120.00"),
                "EUR",
                "pm_card_visa"
        )))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Purchase does not belong to customer");
    }

    @Test
    void should_throw_BusinessRuleException_when_purchase_is_not_in_CREATED_status() {
        PurchaseResponse purchase = createPurchaseCreated(customerId, new BigDecimal("90.00"));
        // First payment (success)
        when(fxRateService.getRateToEur("EUR")).thenReturn(new BigDecimal("1.00"));
        PaymentGateway.AuthorizationResult auth = mock(PaymentGateway.AuthorizationResult.class);
        when(auth.status()).thenReturn(PaymentGateway.GatewayStatus.AUTHORIZED);
        when(auth.authorizationId()).thenReturn("auth_purchase_repeat");
        when(auth.message()).thenReturn("ok");
        PaymentGateway.CaptureResult cap = mock(PaymentGateway.CaptureResult.class);
        when(cap.status()).thenReturn(PaymentGateway.GatewayStatus.PAID);
        when(cap.paymentId()).thenReturn("pi_purchase_repeat");
        when(cap.message()).thenReturn("ok");
        when(paymentGateway.authorize(eq("EUR"), any(BigDecimal.class), eq("pm_card_visa"), startsWith("purchase:")))
                .thenReturn(auth);
        when(paymentGateway.capture("auth_purchase_repeat")).thenReturn(cap);
        paymentService.payPurchase(customerId, new PayPurchaseRequest(
                purchase.id(),
                new BigDecimal("90.00"),
                "EUR",
                "pm_card_visa"
        ));
        // Second payment attempt should fail because purchase is no longer CREATED
        assertThatThrownBy(() -> paymentService.payPurchase(customerId, new PayPurchaseRequest(
                purchase.id(),
                new BigDecimal("90.00"),
                "EUR",
                "pm_card_visa"
        )))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only CREATED purchases can be paid");
    }

    @Test
    void should_throw_BusinessRuleException_when_some_offer_is_no_longer_listed_at_payment_time() {
        PurchaseResponse purchase = createPurchaseCreated(customerId, new BigDecimal("140.00"));
        Long offerId = purchase.items().get(0).saleOfferId();
        // Simulate race: offer became SOLD before payment
        SaleOffer offer = saleOfferRepository.findById(offerId)
                .orElseThrow(() -> new AssertionError("Offer should exist"));
        offer.setStatus(SaleOfferStatus.SOLD);
        saleOfferRepository.saveAndFlush(offer);
        when(fxRateService.getRateToEur("EUR")).thenReturn(new BigDecimal("1.00"));
        PaymentGateway.AuthorizationResult auth = mock(PaymentGateway.AuthorizationResult.class);
        when(auth.status()).thenReturn(PaymentGateway.GatewayStatus.AUTHORIZED);
        when(auth.authorizationId()).thenReturn("auth_purchase_race");
        when(auth.message()).thenReturn("ok");
        when(paymentGateway.authorize(eq("EUR"), any(BigDecimal.class), eq("pm_card_visa"), startsWith("purchase:")))
                .thenReturn(auth);
        PaymentGateway.CaptureResult cap = mock(PaymentGateway.CaptureResult.class);
        when(cap.status()).thenReturn(PaymentGateway.GatewayStatus.PAID);
        when(cap.paymentId()).thenReturn("pi_purchase_race");
        when(cap.message()).thenReturn("ok");
        when(paymentGateway.capture("auth_purchase_race")).thenReturn(cap);
        assertThatThrownBy(() -> paymentService.payPurchase(customerId, new PayPurchaseRequest(
                purchase.id(),
                new BigDecimal("140.00"),
                "EUR",
                "pm_card_visa"
        )))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Offer is no longer available");
    }
    // -------------------------
    // Helpers
    // -------------------------

    private Long createActiveRental(UUID renterId) {
        BikeResponse bike = bikeCatalogService.offerBikeForRent(
                new BikeCreateRequest(
                        "Bike for payment rental",
                        ProviderType.EIFFEL_BIKE_CORP,
                        corpId,
                        new BigDecimal("2.50")
                ),
                corpId
        );
        RentBikeResultResponse r = rentalService.rentBikeOrJoinWaitingList(
                new RentBikeRequest(bike.id(), renterId, 1)
        );
        assertThat(r.result()).isEqualTo(RentResult.RENTED);
        assertThat(r.rentalId()).isNotNull();
        return r.rentalId();
    }

    /**
     * Creates a PURCHASE in CREATED status with 1 item:
     * bike offered -> rented once -> returned -> listed for sale -> added to basket -> checkout.
     */
    private PurchaseResponse createPurchaseCreated(UUID buyerId, BigDecimal askingPriceEur) {
        BikeResponse bike = bikeCatalogService.offerBikeForRent(
                new BikeCreateRequest(
                        "Bike for payment purchase",
                        ProviderType.EIFFEL_BIKE_CORP,
                        corpId,
                        new BigDecimal("2.50")
                ),
                corpId
        );
        // must be rented at least once to be eligible for sale (US_10)
        RentBikeResultResponse rented = rentalService.rentBikeOrJoinWaitingList(
                new RentBikeRequest(bike.id(), buyerId, 1)
        );
        assertThat(rented.result()).isEqualTo(RentResult.RENTED);
        rentalService.returnBike(rented.rentalId(), new ReturnBikeRequest(buyerId, "ok", "GOOD"));
        SaleOfferResponse offer = saleOfferService.createSaleOffer(
                new CreateSaleOfferRequest(bike.id(), corpId, askingPriceEur)
        );
        assertThat(offer.status()).isEqualTo("LISTED");
        BasketResponse basket = basketService.addItem(buyerId, new AddToBasketRequest(offer.id()));
        assertThat(basket.items()).hasSize(1);
        return purchaseService.checkout(buyerId);
    }
}
