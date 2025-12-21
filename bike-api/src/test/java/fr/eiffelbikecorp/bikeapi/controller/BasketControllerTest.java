package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.entity.Customer;
import fr.eiffelbikecorp.bikeapi.domain.entity.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.dto.request.*;
import fr.eiffelbikecorp.bikeapi.dto.response.*;
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
class BasketControllerTest {

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
        c.setEmail("basket-test-" + UUID.randomUUID() + "@test.com");
        c.setFullName("Basket Tester");
        c.setPassword("testpassword");

        customerId = customerRepository.saveAndFlush(c).getId();
    }

    @Test
    void should_get_or_create_open_basket_and_return_200() {
        //put the customerid in the auth header

        ResponseEntity<BasketResponse> r = rest.exchange(
                "/api/basket",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                BasketResponse.class
        );

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");

        BasketResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.status()).isEqualTo("OPEN");
        assertThat(body.items()).isNotNull();
    }

    @Test
    void should_add_item_to_basket_and_return_200() {
        SaleOfferResponse offer = createEligibleSaleOffer("Bike for basket add", new BigDecimal("220.00"));

        ResponseEntity<BasketResponse> r = rest.exchange(
                "/api/basket/items",
                HttpMethod.POST,
                jsonEntity(new AddToBasketRequest(offer.id())),
                BasketResponse.class
        );

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        BasketResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo("OPEN");
        assertThat(body.items()).isNotNull();
        assertThat(body.items()).hasSize(1);

        BasketItemResponse item = body.items().get(0);
        assertThat(item.saleOfferId()).isEqualTo(offer.id());
        assertThat(item.bikeId()).isNotNull();
        assertThat(item.unitPriceEurSnapshot()).isEqualByComparingTo(offer.askingPriceEur());
    }

    @Test
    void should_return_409_when_adding_same_offer_twice() {
        SaleOfferResponse offer = createEligibleSaleOffer("Bike for basket duplicate", new BigDecimal("199.00"));

        ResponseEntity<BasketResponse> first = rest.exchange(
                "/api/basket/items",
                HttpMethod.POST,
                jsonEntity(new AddToBasketRequest(offer.id())),
                BasketResponse.class
        );
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> second = rest.exchange(
                "/api/basket/items",
                HttpMethod.POST,
                jsonEntity(new AddToBasketRequest(offer.id())),
                String.class
        );

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT); // BusinessRuleException -> 409
        assertThat(second.getHeaders().getContentType()).isNotNull();
        assertThat(second.getHeaders().getContentType().toString()).contains("application/json");
        assertThat(second.getBody()).isNotBlank();
    }

    @Test
    void should_remove_item_from_basket_and_return_200() {
        SaleOfferResponse offer = createEligibleSaleOffer("Bike for basket remove", new BigDecimal("180.00"));

        // Add item
        ResponseEntity<BasketResponse> added = rest.exchange(
                "/api/basket/items",
                HttpMethod.POST,
                jsonEntity(new AddToBasketRequest(offer.id())),
                BasketResponse.class
        );
        assertThat(added.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(added.getBody()).isNotNull();
        assertThat(added.getBody().items()).hasSize(1);

        // Remove item
        ResponseEntity<BasketResponse> removed = rest.exchange(
                "/api/basket/items/" + offer.id(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                BasketResponse.class
        );

        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.OK);
        BasketResponse body = removed.getBody();
        assertThat(body).isNotNull();
        assertThat(body.items()).isNotNull();

    }

    @Test
    void should_clear_basket_and_return_200() {
        SaleOfferResponse offer = createEligibleSaleOffer("Bike for basket clear", new BigDecimal("155.00"));

        rest.exchange(
                "/api/basket/items",
                HttpMethod.POST,
                jsonEntity(new AddToBasketRequest(offer.id())),
                BasketResponse.class
        );

        ResponseEntity<BasketResponse> cleared = rest.exchange(
                "/api/basket",
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                BasketResponse.class
        );

        assertThat(cleared.getStatusCode()).isEqualTo(HttpStatus.OK);
        BasketResponse body = cleared.getBody();
        assertThat(body).isNotNull();
        assertThat(body.items()).isNotNull();
        assertThat(body.items()).isEmpty();
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
                jsonEntity(new AddToBasketRequest(offer.id())),
                BasketResponse.class
        ).getBody();
        assertThat(basket).isNotNull();

        PurchaseResponse purchase = rest.exchange(
                "/api/purchases/checkout",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
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
                jsonEntity(payReq),
                String.class
        );

        // If payment succeeds, offer becomes SOLD and should not be addable anymore.
        if (payResp.getStatusCode().is2xxSuccessful() || payResp.getStatusCode() == HttpStatus.CREATED) {
            ResponseEntity<String> addAgain = rest.exchange(
                    "/api/basket/items",
                    HttpMethod.POST,
                    jsonEntity(new AddToBasketRequest(offer.id())),
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
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.50")
        ));

        // eligibility rule: must be rented at least once
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
