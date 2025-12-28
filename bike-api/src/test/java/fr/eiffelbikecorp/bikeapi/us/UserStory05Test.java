package fr.eiffelbikecorp.bikeapi.us;

import fr.eiffelbikecorp.bikeapi.domain.enums.BikeStatus;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.UserType;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.RentBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserLoginRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserRegisterRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.RentBikeResultResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.UserLoginResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.UserResponse;
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
class UserStory05Test {
    // US_05: As a Customer, I want to rent a bike so that I can use it for my commute or daily needs.

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    private String studentToken;
    private UUID studentProviderId;
    private Long bikeId;

    private String customerToken;
    private UUID customerId;

    @BeforeEach
    void setup() {
        String password = "secret123";

        // 1) Create a Student provider + login (to offer a bike)
        String studentEmail = "student+" + UUID.randomUUID() + "@example.com";

        ResponseEntity<UserResponse> studentRegisterResp = rest.exchange(
                API + "/users/register",
                HttpMethod.POST,
                new HttpEntity<>(new UserRegisterRequest(
                        UserType.STUDENT,
                        "Student Provider",
                        studentEmail,
                        password
                ), jsonHeaders()),
                UserResponse.class
        );
        assertThat(studentRegisterResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(studentRegisterResp.getBody()).isNotNull();
        assertThat(studentRegisterResp.getBody().providerId()).isNotNull();
        this.studentProviderId = studentRegisterResp.getBody().providerId();

        ResponseEntity<UserLoginResponse> studentLoginResp = rest.exchange(
                API + "/users/login",
                HttpMethod.POST,
                new HttpEntity<>(new UserLoginRequest(studentEmail, password), jsonHeaders()),
                UserLoginResponse.class
        );
        assertThat(studentLoginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(studentLoginResp.getBody()).isNotNull();
        this.studentToken = studentLoginResp.getBody().accessToken();
        assertThat(studentToken).isNotBlank();

        // Offer one AVAILABLE bike
        ResponseEntity<BikeResponse> bikeCreateResp = rest.exchange(
                API + "/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(new BikeCreateRequest(
                        "Commute bike - available",
                        ProviderType.STUDENT,
                        studentProviderId,
                        new BigDecimal("2.25")
                ), authJsonHeaders(studentToken)),
                BikeResponse.class
        );
        assertThat(bikeCreateResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bikeCreateResp.getBody()).isNotNull();
        this.bikeId = bikeCreateResp.getBody().id();
        assertThat(bikeId).isNotNull();

        // 2) Create a Customer + login (actor of US_05)
        String customerEmail = "customer+" + UUID.randomUUID() + "@example.com";

        ResponseEntity<UserResponse> customerRegisterResp = rest.exchange(
                API + "/users/register",
                HttpMethod.POST,
                new HttpEntity<>(new UserRegisterRequest(
                        UserType.CUSTOMER,
                        "Customer One",
                        customerEmail,
                        password
                ), jsonHeaders()),
                UserResponse.class
        );
        assertThat(customerRegisterResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(customerRegisterResp.getBody()).isNotNull();
        this.customerId = customerRegisterResp.getBody().customerId();
        assertThat(customerId).isNotNull();

        ResponseEntity<UserLoginResponse> customerLoginResp = rest.exchange(
                API + "/users/login",
                HttpMethod.POST,
                new HttpEntity<>(new UserLoginRequest(customerEmail, password), jsonHeaders()),
                UserLoginResponse.class
        );
        assertThat(customerLoginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(customerLoginResp.getBody()).isNotNull();
        this.customerToken = customerLoginResp.getBody().accessToken();
        assertThat(customerToken).isNotBlank();
    }

    @Test
    void should_rent_a_bike_and_return_201() {
        // When: customer rents the bike for 3 days
        RentBikeRequest rentReq = new RentBikeRequest(bikeId, customerId, 3);

        ResponseEntity<RentBikeResultResponse> rentResp = rest.exchange(
                API + "/rentals",
                HttpMethod.POST,
                new HttpEntity<>(rentReq, authJsonHeaders(customerToken)),
                RentBikeResultResponse.class
        );

        // Then: RENTED with 201 Created and a rentalId
        assertThat(rentResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(rentResp.getBody()).isNotNull();
        assertThat(rentResp.getBody().result()).isNotNull();
        assertThat(BikeStatus.valueOf(rentResp.getBody().result().toString())).isEqualTo(BikeStatus.RENTED);
        assertThat(rentResp.getBody().rentalId()).as("rentalId should be present when RENTED").isNotNull();
        assertThat(rentResp.getBody().waitingListEntryId()).as("waitingListEntryId should be null when RENTED").isNull();

        // And: bike should now appear as RENTED in catalog
        ParameterizedTypeReference<List<BikeResponse>> listType = new ParameterizedTypeReference<>() {};
        ResponseEntity<List<BikeResponse>> listResp = rest.exchange(
                API + "/bikes?offeredById=" + studentProviderId,
                HttpMethod.GET,
                new HttpEntity<>(authJsonHeaders(customerToken)),
                listType
        );

        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).isNotNull();

        BikeResponse rentedBike = listResp.getBody().stream()
                .filter(b -> b.id().equals(bikeId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected to find bike " + bikeId + " in provider bikes"));

        assertThat(rentedBike.status()).isEqualTo("RENTED");

        log.info("US_05 OK - customerId={}, bikeId={}, rentalId={}",
                customerId, bikeId, rentResp.getBody().rentalId());
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
