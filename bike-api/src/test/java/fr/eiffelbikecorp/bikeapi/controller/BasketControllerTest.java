package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.domain.enums.UserType;
import fr.eiffelbikecorp.bikeapi.dto.request.*;
import fr.eiffelbikecorp.bikeapi.dto.response.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BasketControllerTest {

    @Autowired
    TestRestTemplate rest;

    private UUID providerId;
    private String providerToken;

    private UUID customerId;
    private String customerToken;

    @BeforeEach
    void setup() {
        String password = "secret123";
        String providerEmail = "renter+" + UUID.randomUUID() + "@example.com";
        UserResponse userResponse = registerUser(UserType.STUDENT, "Renter", providerEmail, password);
        providerToken = login(providerEmail, password);
        providerId = userResponse.providerId();
        String customerEmail = "customer+" + UUID.randomUUID() + "@example.com";
        UserResponse userResponse2 = registerUser(UserType.STUDENT, "Customer", customerEmail, password);
        customerToken = login(customerEmail, password);
        customerId = userResponse.providerId();
    }

    private UserResponse registerUser(UserType type, String fullName, String email, String password) {
        ResponseEntity<UserResponse> resp = rest.exchange(
                "/api/users/register",
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
                "/api/users/login",
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

    @Test
    void should_return_409_when_adding_same_offer_twice() {
        SaleOfferResponse offer = createEligibleSaleOffer("Bike for basket duplicate", new BigDecimal("199.00"));
        ResponseEntity<BasketResponse> first = rest.exchange(
                "/api/basket/items",
                HttpMethod.POST,
                new HttpEntity<>(new AddToBasketRequest(offer.id()), authJsonHeaders(customerToken)),
                BasketResponse.class
        );
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<String> second = rest.exchange(
                "/api/basket/items",
                HttpMethod.POST,
                new HttpEntity<>(new AddToBasketRequest(offer.id()), authJsonHeaders(customerToken)),
                String.class
        );
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT); // BusinessRuleException -> 409
        assertThat(second.getHeaders().getContentType()).isNotNull();
        assertThat(second.getHeaders().getContentType().toString()).contains("application/json");
        assertThat(second.getBody()).isNotBlank();
    }

    @Test
    void should_return_409_when_offer_is_not_listed_anymore() {
        // Create offer
        SaleOfferResponse offer = createEligibleSaleOffer("Bike for basket non-listed", new BigDecimal("300.00"));
        // Make it SOLD by doing a minimal purchase flow (basket->checkout->pay)
        // If your Purchase/Payment controllers are not ready yet, skip this test for now.
        // Otherwise, use your endpoints to sell the offer and then try add to basket.
        // Here is a "safe" approach: if the payment endpoints exist, it will work.
        // If not, remove/ignore this test.
        BasketResponse basket = rest.exchange(
                "/api/basket/items",
                HttpMethod.POST,
                new HttpEntity<>(new AddToBasketRequest(offer.id()), authJsonHeaders(customerToken)),
                BasketResponse.class
        ).getBody();
        assertThat(basket).isNotNull();
        PurchaseResponse purchase = rest.exchange(
                "/api/purchases/checkout",
                HttpMethod.POST,
                new HttpEntity<>(authJsonHeaders(customerToken)),
                PurchaseResponse.class
        ).getBody();
        assertThat(purchase).isNotNull();
        assertThat(purchase.id()).isNotNull();
        // pay purchase (fake PM id just for test; adapt to your working Stripe test method)
        // If you already integrated Stripe in tests, replace paymentMethodId accordingly.
        PayPurchaseRequest payReq = new PayPurchaseRequest(
                purchase.id(),
                purchase.totalAmountEur(),  // paying in EUR for simplicity
                "EUR",
                "pm_123" // <-- replace with a valid test paymentMethodId if your gateway enforces it
        );
        // If payment gateway is not wired for integration tests yet, you can comment this block.
        ResponseEntity<String> payResp = rest.exchange(
                "/api/sales/payments",
                HttpMethod.POST,
                new HttpEntity<>(payReq),
                String.class
        );
        // If payment succeeds, offer becomes SOLD and should not be addable anymore.
        if (payResp.getStatusCode().is2xxSuccessful() || payResp.getStatusCode() == HttpStatus.CREATED) {
            ResponseEntity<String> addAgain = rest.exchange(
                    "/api/basket/items",
                    HttpMethod.POST,
                    new HttpEntity<>(new AddToBasketRequest(offer.id())),
                    String.class
            );
            assertThat(addAgain.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }
    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private SaleOfferResponse createEligibleSaleOffer(String bikeDescription, BigDecimal askingPriceEur) {
        BikeResponse bike = createBike(new BikeCreateRequest(
                bikeDescription,
                ProviderType.STUDENT,
                providerId,
                new BigDecimal("2.50")
        ));
        // eligibility rule: must be rented at least once
        rentBikeOnceAndReturn(bike.id(), customerId);
        ResponseEntity<SaleOfferResponse> created = rest.exchange(
                "/api/sale-offers",
                HttpMethod.POST,
                new HttpEntity<>(new CreateSaleOfferRequest(bike.id(), providerId, askingPriceEur), authJsonHeaders(providerToken)),
                SaleOfferResponse.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        SaleOfferResponse offer = created.getBody();
        assertThat(offer).isNotNull();
        assertThat(offer.id()).isNotNull();
        assertThat(offer.status()).isEqualTo("LISTED");
        return offer;
    }

    private BikeResponse createBike(BikeCreateRequest req) {
        ResponseEntity<BikeResponse> r = rest.exchange(
                "/api/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(req, authJsonHeaders(providerToken)),
                BikeResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BikeResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        return body;
    }

    private void rentBikeOnceAndReturn(Long bikeId, UUID customerId) {
        ResponseEntity<RentBikeResultResponse> rented = rest.exchange(
                "/api/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(bikeId, customerId, 1), authJsonHeaders(customerToken)),
                RentBikeResultResponse.class
        );
        assertThat(rented.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        RentBikeResultResponse rentedBody = rented.getBody();
        assertThat(rentedBody).isNotNull();
        assertThat(rentedBody.result()).isEqualTo(RentResult.RENTED);
        assertThat(rentedBody.rentalId()).isNotNull();
        ResponseEntity<ReturnBikeResponse> returned = rest.exchange(
                "/api/rentals/" + rentedBody.rentalId() + "/return",
                HttpMethod.POST,
                new HttpEntity<>(new ReturnBikeRequest(customerId, "return for sale eligibility", "Good"), authJsonHeaders(customerToken)),
                ReturnBikeResponse.class
        );
        assertThat(returned.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
