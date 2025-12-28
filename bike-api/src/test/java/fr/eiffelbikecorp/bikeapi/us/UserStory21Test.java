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
class UserStory21Test {
    // US_21: As a Customer, I want to view my rent history so that I can track what I rent and when.
    // Maps to: GET /api/rentals  (secured, returns rentals for the authenticated customer)

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository corpRepository;

    private UUID corpProviderId;

    private String operatorToken;

    private String customerToken;
    private UUID customerId;

    private Long bikeId;
    private Long rentalId;

    @BeforeEach
    void setup() {
        String password = "secret123";

        // 0) Ensure corp provider exists
        EiffelBikeCorp corp = corpRepository.save(new EiffelBikeCorp());
        this.corpProviderId = corp.getId();
        assertThat(corpProviderId).isNotNull();

        // 1) Operator user (secured) to create the bike
        String operatorEmail = "operator+" + UUID.randomUUID() + "@example.com";
        registerUser(UserType.CUSTOMER, "Corp Operator", operatorEmail, password);
        this.operatorToken = login(operatorEmail, password);

        // 2) Create a corporate bike available for rent
        ResponseEntity<BikeResponse> bikeCreateResp = rest.exchange(
                API + "/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(new BikeCreateRequest(
                        "Bike for rent history",
                        ProviderType.EIFFEL_BIKE_CORP,
                        corpProviderId,
                        new BigDecimal("2.00")
                ), authJsonHeaders(operatorToken)),
                BikeResponse.class
        );
        assertThat(bikeCreateResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bikeCreateResp.getBody()).isNotNull();
        this.bikeId = bikeCreateResp.getBody().id();
        assertThat(bikeId).isNotNull();

        // 3) Customer registers + logs in (actor of US_21)
        String customerEmail = "customer+" + UUID.randomUUID() + "@example.com";
        UserResponse customer = registerUser(UserType.CUSTOMER, "History Customer", customerEmail, password);
        this.customerId = customer.customerId();
        assertThat(customerId).isNotNull();
        this.customerToken = login(customerEmail, password);

        // 4) Customer rents the bike (so history has at least one rental)
        ResponseEntity<RentBikeResultResponse> rentResp = rest.exchange(
                API + "/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(
                        bikeId,
                        customerId,
                        1
                ), authJsonHeaders(customerToken)),
                RentBikeResultResponse.class
        );

        assertThat(rentResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(rentResp.getBody()).isNotNull();
        assertThat(rentResp.getBody().result()).isEqualTo(RentResult.RENTED);
        this.rentalId = rentResp.getBody().rentalId();
        assertThat(rentalId).isNotNull();

        // 5) Return it (so "when" includes an end time / closed status in many implementations)
        ResponseEntity<ReturnBikeResponse> returnResp = rest.exchange(
                API + "/rentals/" + rentalId + "/return",
                HttpMethod.POST,
                new HttpEntity<>(new ReturnBikeRequest(
                        customerId,
                        "Returned for history test",
                        "OK"
                ), authJsonHeaders(customerToken)),
                ReturnBikeResponse.class
        );
        assertThat(returnResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(returnResp.getBody()).isNotNull();
    }

    @Test
    void should_list_my_rent_history_and_include_dates() {
        ParameterizedTypeReference<List<RentalResponse>> type = new ParameterizedTypeReference<>() {};

        ResponseEntity<List<RentalResponse>> listResp = rest.exchange(
                API + "/rentals",
                HttpMethod.GET,
                new HttpEntity<>(authJsonHeaders(customerToken)),
                type
        );

        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getHeaders().getContentType()).isNotNull();
        assertThat(listResp.getHeaders().getContentType().toString()).contains("application/json");

        List<RentalResponse> rentals = listResp.getBody();
        assertThat(rentals).isNotNull();
        assertThat(rentals).isNotEmpty();

        RentalResponse mine = rentals.stream()
                .filter(r -> r.id().equals(rentalId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected rental " + rentalId + " in rent history"));

        assertThat(mine.bikeId()).isEqualTo(bikeId);

        // "when": at least start date must exist
        assertThat(mine.startAt()).isNotNull();

        // after return, endAt should typically be present; if your implementation keeps it null, remove this assert
        assertThat(mine.endAt()).isNotNull();

        // status should be present (ACTIVE/CLOSED/etc.)
        assertThat(mine.status()).isNotBlank();

        log.info("US_21 OK - rentalId={}, bikeId={}, startAt={}, endAt={}, status={}",
                mine.id(), mine.bikeId(), mine.startAt(), mine.endAt(), mine.status());
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
