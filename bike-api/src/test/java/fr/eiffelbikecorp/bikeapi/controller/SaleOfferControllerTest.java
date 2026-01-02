package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.domain.enums.UserType;
import fr.eiffelbikecorp.bikeapi.dto.request.*;
import fr.eiffelbikecorp.bikeapi.dto.response.*;
import fr.eiffelbikecorp.bikeapi.persistence.StudentRepository;
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
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SaleOfferControllerTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    StudentRepository studentRepository;

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
    void should_return_409_when_bike_was_never_rented() {
        BikeResponse bike = createBike(new BikeCreateRequest(
                "Corp bike never rented",
                ProviderType.STUDENT,
                providerId,
                new BigDecimal("2.50")
        ));
        CreateSaleOfferRequest req = new CreateSaleOfferRequest(
                bike.id(),
                providerId,
                new BigDecimal("250.00")
        );
        ResponseEntity<String> r = rest.exchange(
                "/api/sale-offers",
                HttpMethod.POST,
                new HttpEntity<>(req, authJsonHeaders(providerToken)),
                String.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody()).isNotBlank();
    }

    @Test
    void should_return_400_when_create_sale_offer_request_is_invalid() {
        CreateSaleOfferRequest invalid = new CreateSaleOfferRequest(null, null, null);
        ResponseEntity<String> r = rest.exchange(
                "/api/sale-offers",
                HttpMethod.POST,
                new HttpEntity<>(invalid, authJsonHeaders(providerToken)),
                String.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody()).isNotBlank();
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

    private SaleOfferResponse createSaleOffer(Long bikeId, UUID sellerCorpId, BigDecimal askingPriceEur) {
        ResponseEntity<SaleOfferResponse> r = rest.exchange(
                "/api/sale-offers",
                HttpMethod.POST,
                new HttpEntity<>(new CreateSaleOfferRequest(bikeId, sellerCorpId, askingPriceEur)),
                SaleOfferResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        SaleOfferResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        return body;
    }

    private void rentBikeOnceAndReturn(Long bikeId, UUID customerId) {
        // Rent
        ResponseEntity<RentBikeResultResponse> rented = rest.exchange(
                "/api/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(bikeId, customerId, 1)),
                RentBikeResultResponse.class
        );
        assertThat(rented.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        RentBikeResultResponse rentedBody = rented.getBody();
        assertThat(rentedBody).isNotNull();
        assertThat(rentedBody.result()).isEqualTo(RentResult.RENTED);
        assertThat(rentedBody.rentalId()).isNotNull();
        // Return (so bike becomes available again for other tests)
        ResponseEntity<ReturnBikeResponse> returned = rest.exchange(
                "/api/rentals/" + rentedBody.rentalId() + "/return",
                HttpMethod.POST,
                new HttpEntity<>(new ReturnBikeRequest(customerId, "return for sale eligibility", "Good")),
                ReturnBikeResponse.class
        );
        assertThat(returned.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
