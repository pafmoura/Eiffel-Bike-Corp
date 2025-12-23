package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.entity.Customer;
import fr.eiffelbikecorp.bikeapi.domain.entity.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.dto.request.*;
import fr.eiffelbikecorp.bikeapi.dto.response.*;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
import fr.eiffelbikecorp.bikeapi.security.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
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
class PurchaseControllerTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository corpRepository;

    @Autowired
    CustomerRepository customerRepository;

    private UUID corpId;
    private UUID customerId;

    @Autowired
    private TokenService tokenService;
    private String accessToken;

    @BeforeEach
    void setup() {
        EiffelBikeCorp corp = new EiffelBikeCorp();
        corpId = corpRepository.saveAndFlush(corp).getId();
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setEmail("purchase-test-" + UUID.randomUUID() + "@test.com");
        c.setFullName("Purchase Tester");
        c.setPassword("testpassword");
        c = customerRepository.saveAndFlush(c);
        customerId = c.getId();
        accessToken = tokenService.generateToken(c);
    }

    @Test
    void should_checkout_basket_and_return_201_with_purchase_body() {
        SaleOfferResponse offer = createEligibleSaleOffer("Bike for purchase checkout", new BigDecimal("240.00"));
        // add to basket
        ResponseEntity<BasketResponse> basket = rest.exchange(
                "/api/basket/items",
                HttpMethod.POST,
                jsonEntity(new AddToBasketRequest(offer.id())),
                BasketResponse.class
        );
        assertThat(basket.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(basket.getBody()).isNotNull();
        assertThat(basket.getBody().items()).hasSize(1);
        // checkout
        ResponseEntity<PurchaseResponse> checkout = rest.exchange(
                "/api/purchases/checkout",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                PurchaseResponse.class
        );
        assertThat(checkout.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(checkout.getHeaders().getContentType()).isNotNull();
        assertThat(checkout.getHeaders().getContentType().toString()).contains("application/json");
        PurchaseResponse body = checkout.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.status()).isEqualTo("CREATED");
        assertThat(body.totalAmountEur()).isNotNull();
        assertThat(body.totalAmountEur()).isEqualByComparingTo(offer.askingPriceEur());
        assertThat(body.items()).isNotNull();
        assertThat(body.items()).hasSize(1);
        assertThat(body.items().get(0).saleOfferId()).isEqualTo(offer.id());
    }

    @Test
    void should_return_409_when_checkout_with_empty_basket() {
        // Ensure basket exists but is empty
        ResponseEntity<BasketResponse> openBasket = rest.exchange(
                "/api/basket",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                BasketResponse.class
        );
        assertThat(openBasket.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(openBasket.getBody()).isNotNull();
        assertThat(openBasket.getBody().items()).isEmpty();
        ResponseEntity<String> checkout = rest.exchange(
                "/api/purchases/checkout",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                String.class
        );
        assertThat(checkout.getStatusCode()).isEqualTo(HttpStatus.CONFLICT); // BusinessRuleException -> 409
        assertThat(checkout.getHeaders().getContentType()).isNotNull();
        assertThat(checkout.getHeaders().getContentType().toString()).contains("application/json");
        assertThat(checkout.getBody()).isNotBlank();
    }

    @Test
    void should_list_purchases_and_return_200() {
        // Create a purchase to list
        SaleOfferResponse offer = createEligibleSaleOffer("Bike for purchase list", new BigDecimal("199.00"));
        rest.exchange(
                "/api/basket/items",
                HttpMethod.POST,
                jsonEntity(new AddToBasketRequest(offer.id())),
                BasketResponse.class
        );
        rest.exchange(
                "/api/purchases/checkout",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                PurchaseResponse.class
        );
        ResponseEntity<List<PurchaseResponse>> r = rest.exchange(
                "/api/purchases",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        List<PurchaseResponse> body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body).isNotEmpty();
    }

    @Test
    void should_get_purchase_by_id_and_return_200() {
        SaleOfferResponse offer = createEligibleSaleOffer("Bike for purchase getById", new BigDecimal("210.00"));
        rest.exchange(
                "/api/basket/items",
                HttpMethod.POST,
                jsonEntity(new AddToBasketRequest(offer.id())),
                BasketResponse.class
        );
        PurchaseResponse created = rest.exchange(
                "/api/purchases/checkout",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                PurchaseResponse.class
        ).getBody();
        assertThat(created).isNotNull();
        ResponseEntity<PurchaseResponse> fetched = rest.exchange(
                "/api/purchases/" + created.id(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                PurchaseResponse.class
        );
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        PurchaseResponse body = fetched.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(created.id());
        assertThat(body.items()).isNotNull();
        assertThat(body.items()).hasSize(1);
        assertThat(body.items().get(0).saleOfferId()).isEqualTo(offer.id());
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
        headers.setBearerAuth(accessToken); // token = customer UUID
        return headers;
    }

    private <T> HttpEntity<T> jsonEntity(T body) {
        return new HttpEntity<>(body, authHeaders());
    }
}
