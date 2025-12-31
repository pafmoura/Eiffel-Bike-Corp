package fr.eiffelbikecorp.bikeapi.us;

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
class UserStory12Test {
    // US_12: As a Customer,
    // I want to search for bikes available to buy
    // so that I can find a bike to purchase.

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository corpRepository;

    private UUID corpProviderId;
    private String providerToken;
    private String customerToken;

    private Long saleOfferId;
    private Long bikeId;

    @BeforeEach
    void setup() {
        String password = "secret123";
        String operatorEmail = "operator+" + UUID.randomUUID() + "@example.com";
        UserResponse operator = registerUser(UserType.EIFFEL_BIKE_CORP, "Corp Operator", operatorEmail, password);
        this.providerToken = login(operatorEmail, password);
        this.corpProviderId = operator.providerId();
        String renterEmail = "renter+" + UUID.randomUUID() + "@example.com";
        UserResponse renter = registerUser(UserType.STUDENT, "Renter", renterEmail, password);
        String renterToken = login(renterEmail, password);
        ResponseEntity<BikeResponse> bikeCreateResp = rest.exchange(
                API + "/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(new BikeCreateRequest(
                        "Used corp bike for sale - black",
                        ProviderType.EIFFEL_BIKE_CORP,
                        corpProviderId,
                        new BigDecimal("1.50")
                ), authJsonHeaders(providerToken)),
                BikeResponse.class
        );
        assertThat(bikeCreateResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bikeCreateResp.getBody()).isNotNull();
        this.bikeId = bikeCreateResp.getBody().id();
        assertThat(bikeId).isNotNull();
        ResponseEntity<RentBikeResultResponse> rentResp = rest.exchange(
                API + "/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(bikeId, renter.customerId(), 1), authJsonHeaders(renterToken)),
                RentBikeResultResponse.class
        );
        assertThat(rentResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(rentResp.getBody()).isNotNull();
        assertThat(rentResp.getBody().result()).isEqualTo(RentResult.RENTED);
        Long rentalId = rentResp.getBody().rentalId();
        assertThat(rentalId).isNotNull();
        ResponseEntity<ReturnBikeResponse> returnResp = rest.exchange(
                API + "/rentals/" + rentalId + "/return",
                HttpMethod.POST,
                new HttpEntity<>(new ReturnBikeRequest(renter.customerId(), "Returned.", "OK"), authJsonHeaders(renterToken)),
                ReturnBikeResponse.class
        );
        assertThat(returnResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<SaleOfferResponse> offerResp = rest.exchange(
                API + "/sale-offers",
                HttpMethod.POST,
                new HttpEntity<>(new CreateSaleOfferRequest(
                        bikeId,
                        corpProviderId,
                        new BigDecimal("99.00")
                ), authJsonHeaders(providerToken)),
                SaleOfferResponse.class
        );
        assertThat(offerResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(offerResp.getBody()).isNotNull();
        this.saleOfferId = offerResp.getBody().id();
        assertThat(saleOfferId).isNotNull();
        // Customer actor
        String customerEmail = "buyer+" + UUID.randomUUID() + "@example.com";
        registerUser(UserType.CUSTOMER, "Buyer", customerEmail, password);
        this.customerToken = login(customerEmail, password);
    }

    @Test
    void should_search_sale_offers_and_return_200() {
        ParameterizedTypeReference<List<SaleOfferResponse>> type = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<List<SaleOfferResponse>> resp = rest.exchange(
                API + "/sale-offers?q=Used",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()), // public endpoint
                type
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody())
                .extracting(SaleOfferResponse::id)
                .contains(saleOfferId);
        SaleOfferResponse found = resp.getBody().stream()
                .filter(o -> o.id().equals(saleOfferId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected to find sale offer " + saleOfferId + " in search results"));
        assertThat(found.bikeId()).isEqualTo(bikeId);
        assertThat(found.askingPriceEur()).isEqualByComparingTo("99.00");
        assertThat(String.valueOf(found.status())).isEqualTo("LISTED");
        log.info("US_12 OK - search returned saleOfferId={}, bikeId={}", saleOfferId, bikeId);
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
