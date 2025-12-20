package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.Customer;
import fr.eiffelbikecorp.bikeapi.domain.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.dto.*;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(MockitoExtension.class)
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SalePaymentControllerTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository corpRepository;

    @Autowired
    CustomerRepository customerRepository;

    private UUID corpId;
    private UUID customerId;

    @BeforeEach
    void setup() {
        EiffelBikeCorp corp = new EiffelBikeCorp();
        corpId = corpRepository.saveAndFlush(corp).getId();

        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setEmail("sale-payment-test-" + UUID.randomUUID() + "@test.com");
        c.setFullName("Sale Payment Tester");
        customerId = customerRepository.saveAndFlush(c).getId();
    }

    @Test
    void should_pay_purchase_and_return_201_with_response_body() {
        // 1) Create eligible sale offer + add to basket + checkout purchase
        SaleOfferResponse offer = createEligibleSaleOffer("Bike for purchase payment", new BigDecimal("240.00"));

        rest.exchange(
                "/api/basket/items",
                HttpMethod.POST,
                jsonEntity(new AddToBasketRequest(offer.id())),
                BasketResponse.class
        );

        ResponseEntity<PurchaseResponse> checkout = rest.exchange(
                "/api/purchases/checkout",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                PurchaseResponse.class
        );
        assertThat(checkout.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        PurchaseResponse purchase = checkout.getBody();
        assertThat(purchase).isNotNull();
        assertThat(purchase.id()).isNotNull();
        assertThat(purchase.totalAmountEur()).isEqualByComparingTo(offer.askingPriceEur());

        // 2) Pay
        // If Stripe is used for real, replace paymentMethodId with a valid fresh one.
        String paymentMethodId = "pm_test_visa";

        PayPurchaseRequest payReq = new PayPurchaseRequest(
                purchase.id(),
                purchase.totalAmountEur(), // simplest: pay exact total in EUR
                "EUR",
                paymentMethodId
        );

        ResponseEntity<SalePaymentResponse> paid = rest.exchange(
                "/api/payments/purchases",
                HttpMethod.POST,
                jsonEntity(payReq),
                SalePaymentResponse.class
        );

        assertThat(paid.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(paid.getHeaders().getContentType()).isNotNull();
        assertThat(paid.getHeaders().getContentType().toString()).contains("application/json");

        SalePaymentResponse body = paid.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.purchaseId()).isEqualTo(purchase.id());
        assertThat(body.status()).isEqualTo("PAID");
        assertThat(body.amountEur()).isNotNull();
        assertThat(body.paidAt()).isNotNull();

        // Optional: confirm offer is now SOLD (if your service sets it on successful payment)
        ResponseEntity<SaleOfferDetailsResponse> offerDetails = rest.exchange(
                "/api/sales/offers/" + offer.id(),
                HttpMethod.GET,
                null,
                SaleOfferDetailsResponse.class
        );
        assertThat(offerDetails.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(offerDetails.getBody()).isNotNull();
        assertThat(offerDetails.getBody().offer().status()).isEqualTo("SOLD");
    }

    @Test
    void should_return_409_when_paying_same_purchase_twice() {
        SaleOfferResponse offer = createEligibleSaleOffer("Bike for double-pay", new BigDecimal("199.00"));

        rest.exchange(
                "/api/basket/items",
                HttpMethod.POST,
                jsonEntity(new AddToBasketRequest(offer.id())),
                BasketResponse.class
        );

        PurchaseResponse purchase = rest.exchange(
                "/api/purchases/checkout",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                PurchaseResponse.class
        ).getBody();
        assertThat(purchase).isNotNull();

        PayPurchaseRequest payReq = new PayPurchaseRequest(
                purchase.id(),
                purchase.totalAmountEur(),
                "EUR",
                "pm_test_visa"
        );

        // First payment -> 201
        ResponseEntity<String> first = rest.exchange(
                "/api/payments/purchases",
                HttpMethod.POST,
                jsonEntity(payReq),
                String.class
        );
        assertThat(first.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.OK);

        // Second payment -> 409 (BusinessRuleException)
        ResponseEntity<String> second = rest.exchange(
                "/api/payments/purchases",
                HttpMethod.POST,
                jsonEntity(payReq),
                String.class
        );
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getHeaders().getContentType()).isNotNull();
        assertThat(second.getHeaders().getContentType().toString()).contains("application/json");
        assertThat(second.getBody()).isNotBlank();
    }

    @Test
    void should_return_400_when_pay_purchase_request_is_invalid() {
        PayPurchaseRequest invalid = new PayPurchaseRequest(
                null,
                null,
                "",
                ""
        );

        ResponseEntity<String> r = rest.exchange(
                "/api/payments/purchases",
                HttpMethod.POST,
                jsonEntity(invalid),
                String.class
        );

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        assertThat(r.getBody()).isNotBlank();
    }

    // Optional but useful
    @Test
    void should_return_404_when_purchase_not_found() {
        PayPurchaseRequest req = new PayPurchaseRequest(
                999999L,
                new BigDecimal("10.00"),
                "EUR",
                "pm_test_visa"
        );

        ResponseEntity<String> r = rest.exchange(
                "/api/payments/purchases",
                HttpMethod.POST,
                jsonEntity(req),
                String.class
        );

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody()).isNotBlank();
    }

    // -------------------------------------------------------
    // Helpers (same pattern you used before)
    // -------------------------------------------------------

    private SaleOfferResponse createEligibleSaleOffer(String bikeDescription, BigDecimal askingPriceEur) {
        BikeResponse bike = createBike(new BikeCreateRequest(
                bikeDescription,
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.50")
        ));

        // Eligibility rule: must be rented at least once
        rentBikeOnceAndReturn(bike.id(), customerId);

        ResponseEntity<SaleOfferResponse> created = rest.exchange(
                "/api/sales/offers",
                HttpMethod.POST,
                jsonEntity(new CreateSaleOfferRequest(bike.id(), corpId, askingPriceEur)),
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
                "/api/bikes",
                HttpMethod.POST,
                jsonEntity(req),
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
                jsonEntity(new RentBikeRequest(bikeId, customerId, 1)),
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
                jsonEntity(new ReturnBikeRequest(customerId, "return for sale eligibility", "Good")),
                ReturnBikeResponse.class
        );
        assertThat(returned.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(customerId.toString()); // token = customer UUID
        return headers;
    }

    private <T> HttpEntity<T> jsonEntity(T body) {
        return new HttpEntity<>(body, authHeaders());
    }
}
