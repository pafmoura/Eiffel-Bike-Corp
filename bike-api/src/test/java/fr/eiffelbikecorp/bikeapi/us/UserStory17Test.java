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
class UserStory17Test {
    // US_17: As a Customer, I want to remove bikes from my basket so that I can adjust my selection.

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository corpRepository;

    private UUID corpProviderId;

    private String operatorToken;
    private Long bikeId;
    private Long saleOfferId;

    private String customerToken;

    @BeforeEach
    void setup() {
        String password = "secret123";

        // 0) Ensure corp provider exists
        EiffelBikeCorp corp = corpRepository.save(new EiffelBikeCorp());
        this.corpProviderId = corp.getId();
        assertThat(corpProviderId).isNotNull();

        // 1) Operator creates a sale offer
        String operatorEmail = "operator+" + UUID.randomUUID() + "@example.com";
        registerUser(UserType.CUSTOMER, "Corp Operator", operatorEmail, password);
        this.operatorToken = login(operatorEmail, password);

        // 2) Renter rents once + returns (compat with US_10 rule)
        String renterEmail = "renter+" + UUID.randomUUID() + "@example.com";
        UserResponse renter = registerUser(UserType.CUSTOMER, "Renter", renterEmail, password);
        String renterToken = login(renterEmail, password);

        // Create corporate bike
        ResponseEntity<BikeResponse> bikeCreate = rest.exchange(
                API + "/bikes",
                HttpMethod.POST,
                new HttpEntity<>(new BikeCreateRequest(
                        "Corp bike to remove from basket",
                        ProviderType.EIFFEL_BIKE_CORP,
                        corpProviderId,
                        new BigDecimal("1.50")
                ), authJsonHeaders(operatorToken)),
                BikeResponse.class
        );
        assertThat(bikeCreate.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bikeCreate.getBody()).isNotNull();
        this.bikeId = bikeCreate.getBody().id();
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
                API + "/sale-offers",
                HttpMethod.POST,
                new HttpEntity<>(new CreateSaleOfferRequest(
                        bikeId,
                        corpProviderId,
                        new BigDecimal("110.00")
                ), authJsonHeaders(operatorToken)),
                SaleOfferResponse.class
        );
        assertThat(offer.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(offer.getBody()).isNotNull();
        this.saleOfferId = offer.getBody().id();
        assertThat(saleOfferId).isNotNull();

        // 3) Customer registers + logs in, then adds the offer to basket
        String customerEmail = "customer+" + UUID.randomUUID() + "@example.com";
        registerUser(UserType.CUSTOMER, "Buyer Customer", customerEmail, password);
        this.customerToken = login(customerEmail, password);

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
    }

    @Test
    void should_remove_item_from_basket_and_return_200() {
        // When: customer removes the sale offer from the basket
        ResponseEntity<BasketResponse> removeResp = rest.exchange(
                API + "/basket/items/" + saleOfferId,
                HttpMethod.DELETE,
                new HttpEntity<>(authJsonHeaders(customerToken)),
                BasketResponse.class
        );

        // Then: basket returned without that item
        assertThat(removeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(removeResp.getBody()).isNotNull();
        assertThat(removeResp.getBody().items()).isNotNull();

        assertThat(removeResp.getBody().items())
                .extracting(BasketItemResponse::saleOfferId)
                .doesNotContain(saleOfferId);

        // And: GET /basket also shows it is removed
        ResponseEntity<BasketResponse> getResp = rest.exchange(
                API + "/basket",
                HttpMethod.GET,
                new HttpEntity<>(authJsonHeaders(customerToken)),
                BasketResponse.class
        );

        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp.getBody()).isNotNull();
        assertThat(getResp.getBody().items())
                .extracting(BasketItemResponse::saleOfferId)
                .doesNotContain(saleOfferId);

        log.info("US_17 OK - removed saleOfferId={} from basketId={}", saleOfferId, removeResp.getBody().id());
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
