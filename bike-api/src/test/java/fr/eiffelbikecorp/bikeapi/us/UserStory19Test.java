package fr.eiffelbikecorp.bikeapi.us;

import fr.eiffelbikecorp.bikeapi.domain.entity.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.domain.enums.UserType;
import fr.eiffelbikecorp.bikeapi.dto.request.*;
import fr.eiffelbikecorp.bikeapi.dto.response.*;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(MockitoExtension.class)
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserStory19Test {
    // US_19: As a Customer, I want to pay for my purchase through a payment gateway
    //        so that the system can verify funds and complete the payment.

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository corpRepository;

    private UUID corpProviderId;
    private String operatorToken;

    private String customerToken;
    private Long purchaseId;
    private BigDecimal purchaseTotalEur;

    @BeforeEach
    void setup() {
        String password = "secret123";
        // 0) Ensure corp provider exists
        EiffelBikeCorp corp = corpRepository.save(new EiffelBikeCorp());
        this.corpProviderId = corp.getId();
        assertThat(corpProviderId).isNotNull();
        // 1) Operator (secured) creates bike + sale offer
        String operatorEmail = "operator+" + UUID.randomUUID() + "@example.com";
        registerUser(UserType.CUSTOMER, "Corp Operator", operatorEmail, password);
        this.operatorToken = login(operatorEmail, password);
        // 2) Renter rents once + returns (compat with rule: only used corp bikes can be sold)
        String renterEmail = "renter+" + UUID.randomUUID() + "@example.com";
        UserResponse renter = registerUser(UserType.CUSTOMER, "Renter", renterEmail, password);
        String renterToken = login(renterEmail, password);
        // Create corporate bike
        ResponseEntity<BikeResponse> bikeCreate = rest.exchange(
                API + "/bikes",
                HttpMethod.POST,
                new HttpEntity<>(new BikeCreateRequest(
                        "Corp bike to be purchased (paid via gateway)",
                        ProviderType.EIFFEL_BIKE_CORP,
                        corpProviderId,
                        new BigDecimal("1.50")
                ), authJsonHeaders(operatorToken)),
                BikeResponse.class
        );
        assertThat(bikeCreate.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bikeCreate.getBody()).isNotNull();
        Long bikeId = bikeCreate.getBody().id();
        assertThat(bikeId).isNotNull();
        // Rent once + return
        ResponseEntity<RentBikeResultResponse> rent = rest.exchange(
                API + "/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(bikeId, renter.customerId(), 1), authJsonHeaders(renterToken)),
                RentBikeResultResponse.class
        );
        assertThat(rent.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(rent.getBody()).isNotNull();
        assertThat(rent.getBody().result()).isEqualTo(RentResult.RENTED);
        Long rentalId = rent.getBody().rentalId();
        assertThat(rentalId).isNotNull();
        ResponseEntity<ReturnBikeResponse> returned = rest.exchange(
                API + "/rentals/" + rentalId + "/return",
                HttpMethod.POST,
                new HttpEntity<>(new ReturnBikeRequest(renter.customerId(), "Returned.", "OK"), authJsonHeaders(renterToken)),
                ReturnBikeResponse.class
        );
        assertThat(returned.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Create sale offer
        ResponseEntity<SaleOfferResponse> offer = rest.exchange(
                API + "/sales/offers",
                HttpMethod.POST,
                new HttpEntity<>(new CreateSaleOfferRequest(
                        bikeId,
                        corpProviderId,
                        new BigDecimal("199.00")
                ), authJsonHeaders(operatorToken)),
                SaleOfferResponse.class
        );
        assertThat(offer.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(offer.getBody()).isNotNull();
        Long saleOfferId = offer.getBody().id();
        assertThat(saleOfferId).isNotNull();
        // 3) Buyer customer creates basket + checkout => purchase created
        String customerEmail = "buyer+" + UUID.randomUUID() + "@example.com";
        UserResponse buyer = registerUser(UserType.CUSTOMER, "Buyer Customer", customerEmail, password);
        this.customerToken = login(customerEmail, password);
        assertThat(customerToken).isNotBlank();
        // Add to basket
        ResponseEntity<BasketResponse> addResp = rest.exchange(
                API + "/basket/items",
                HttpMethod.POST,
                new HttpEntity<>(new AddToBasketRequest(saleOfferId), authJsonHeaders(customerToken)),
                BasketResponse.class
        );
        assertThat(addResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(addResp.getBody()).isNotNull();
        assertThat(addResp.getBody().items())
                .extracting(BasketItemResponse::saleOfferId)
                .contains(saleOfferId);
        // Checkout
        ResponseEntity<PurchaseResponse> checkoutResp = rest.exchange(
                API + "/purchases/checkout",
                HttpMethod.POST,
                new HttpEntity<>(authJsonHeaders(customerToken)),
                PurchaseResponse.class
        );
        assertThat(checkoutResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(checkoutResp.getBody()).isNotNull();
        assertThat(checkoutResp.getBody().id()).isNotNull();
        this.purchaseId = checkoutResp.getBody().id();
        this.purchaseTotalEur = checkoutResp.getBody().totalAmountEur();
        assertThat(purchaseTotalEur).isNotNull();
        assertThat(purchaseTotalEur).isGreaterThan(BigDecimal.ZERO);
        // sanity: initial status is CREATED
        assertThat(checkoutResp.getBody().status()).isEqualTo("CREATED");
        log.info("Setup OK - buyerCustomerId={}, purchaseId={}, totalEur={}",
                buyer.customerId(), purchaseId, purchaseTotalEur);
    }

    @Test
    void should_pay_purchase_through_gateway_and_return_201_and_mark_purchase_as_paid() {
        // Given: PayPurchaseRequest (EUR to avoid FX mismatch here; FX is covered in US_08)
        PayPurchaseRequest payReq = new PayPurchaseRequest(
                purchaseId,
                purchaseTotalEur,
                "EUR",
                "pm_card_visa"
        );
        // When: customer pays the purchase via payment gateway
        ResponseEntity<SalePaymentResponse> payResp = rest.exchange(
                API + "/payments/purchases",
                HttpMethod.POST,
                new HttpEntity<>(payReq, authJsonHeaders(customerToken)),
                SalePaymentResponse.class
        );
        // Then: payment created
        assertThat(payResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(payResp.getBody()).isNotNull();
        SalePaymentResponse payment = payResp.getBody();
        assertThat(payment.id()).isNotNull();
        assertThat(payment.purchaseId()).isEqualTo(purchaseId);
        assertThat(payment.originalCurrency()).isEqualTo("EUR");
        assertThat(payment.originalAmount()).isEqualByComparingTo(purchaseTotalEur);
        assertThat(payment.amountEur()).isNotNull();
        assertThat(payment.amountEur()).isEqualByComparingTo(purchaseTotalEur);
        assertThat(payment.status()).isEqualTo("PAID");
        assertThat(payment.paidAt()).isNotNull();
        assertThat(payment.stripePaymentIntentId()).isNotBlank();
        // And: purchase is now PAID
        ResponseEntity<PurchaseResponse> purchaseResp = rest.exchange(
                API + "/purchases/" + purchaseId,
                HttpMethod.GET,
                new HttpEntity<>(authJsonHeaders(customerToken)),
                PurchaseResponse.class
        );
        assertThat(purchaseResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(purchaseResp.getBody()).isNotNull();
        assertThat(purchaseResp.getBody().id()).isEqualTo(purchaseId);
        assertThat(purchaseResp.getBody().status()).isEqualTo("PAID");
        assertThat(purchaseResp.getBody().paidAt()).isNotNull();
        log.info("US_19 OK - purchaseId={}, paymentId={}, paymentIntentId={}",
                purchaseId, payment.id(), payment.stripePaymentIntentId());
    }

    private UserResponse registerUser(UserType type, String fullName, String email, String password) {
        ResponseEntity<UserResponse> resp = rest.exchange(
                API + "/users/register",
                HttpMethod.POST,
                new HttpEntity<>(new UserRegisterRequest(type, fullName, email, password), jsonHeaders()),
                UserResponse.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        return resp.getBody();
    }

    private String login(String email, String password) {
        ResponseEntity<UserLoginResponse> resp = rest.exchange(
                API + "/users/login",
                HttpMethod.POST,
                new HttpEntity<>(new UserLoginRequest(email, password), jsonHeaders()),
                UserLoginResponse.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().accessToken()).isNotBlank();
        return resp.getBody().accessToken();
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        return h;
    }

    private static HttpHeaders authJsonHeaders(String token) {
        HttpHeaders h = jsonHeaders();
        h.setBearerAuth(token);
        return h;
    }
}
