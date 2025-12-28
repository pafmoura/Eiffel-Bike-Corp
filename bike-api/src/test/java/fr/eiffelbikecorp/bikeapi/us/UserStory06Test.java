package fr.eiffelbikecorp.bikeapi.us;

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
class UserStory06Test {
    // US_06: As a Customer, I want to be added to a waiting list when a bike is unavailable
    //        so that I can rent it as soon as it becomes available.

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    private String studentToken;
    private UUID studentProviderId;
    private Long bikeId;

    private String customerAToken;
    private UUID customerAId;

    private String customerBToken;
    private UUID customerBId;

    @BeforeEach
    void setup() {
        String password = "secret123";

        // 1) Student offers one bike
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

        ResponseEntity<BikeResponse> bikeCreateResp = rest.exchange(
                API + "/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(new BikeCreateRequest(
                        "Popular bike - will be rented",
                        ProviderType.STUDENT,
                        studentProviderId,
                        new BigDecimal("2.00")
                ), authJsonHeaders(studentToken)),
                BikeResponse.class
        );
        assertThat(bikeCreateResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bikeCreateResp.getBody()).isNotNull();
        this.bikeId = bikeCreateResp.getBody().id();
        assertThat(bikeId).isNotNull();

        // 2) Customer A registers + logs in (will rent the bike first)
        String customerAEmail = "customerA+" + UUID.randomUUID() + "@example.com";

        ResponseEntity<UserResponse> customerARegisterResp = rest.exchange(
                API + "/users/register",
                HttpMethod.POST,
                new HttpEntity<>(new UserRegisterRequest(
                        UserType.CUSTOMER,
                        "Customer A",
                        customerAEmail,
                        password
                ), jsonHeaders()),
                UserResponse.class
        );
        assertThat(customerARegisterResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(customerARegisterResp.getBody()).isNotNull();
        this.customerAId = customerARegisterResp.getBody().customerId();
        assertThat(customerAId).isNotNull();

        ResponseEntity<UserLoginResponse> customerALoginResp = rest.exchange(
                API + "/users/login",
                HttpMethod.POST,
                new HttpEntity<>(new UserLoginRequest(customerAEmail, password), jsonHeaders()),
                UserLoginResponse.class
        );
        assertThat(customerALoginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(customerALoginResp.getBody()).isNotNull();
        this.customerAToken = customerALoginResp.getBody().accessToken();
        assertThat(customerAToken).isNotBlank();

        // 3) Customer B registers + logs in (will attempt to rent and get waitlisted)
        String customerBEmail = "customerB+" + UUID.randomUUID() + "@example.com";

        ResponseEntity<UserResponse> customerBRegisterResp = rest.exchange(
                API + "/users/register",
                HttpMethod.POST,
                new HttpEntity<>(new UserRegisterRequest(
                        UserType.CUSTOMER,
                        "Customer B",
                        customerBEmail,
                        password
                ), jsonHeaders()),
                UserResponse.class
        );
        assertThat(customerBRegisterResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(customerBRegisterResp.getBody()).isNotNull();
        this.customerBId = customerBRegisterResp.getBody().customerId();
        assertThat(customerBId).isNotNull();

        ResponseEntity<UserLoginResponse> customerBLoginResp = rest.exchange(
                API + "/users/login",
                HttpMethod.POST,
                new HttpEntity<>(new UserLoginRequest(customerBEmail, password), jsonHeaders()),
                UserLoginResponse.class
        );
        assertThat(customerBLoginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(customerBLoginResp.getBody()).isNotNull();
        this.customerBToken = customerBLoginResp.getBody().accessToken();
        assertThat(customerBToken).isNotBlank();

        // Make the bike unavailable: Customer A rents it
        RentBikeRequest rentByA = new RentBikeRequest(bikeId, customerAId, 2);

        ResponseEntity<RentBikeResultResponse> rentAResp = rest.exchange(
                API + "/rentals",
                HttpMethod.POST,
                new HttpEntity<>(rentByA, authJsonHeaders(customerAToken)),
                RentBikeResultResponse.class
        );

        assertThat(rentAResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(rentAResp.getBody()).isNotNull();
        assertThat(rentAResp.getBody().result().name()).isEqualTo("RENTED");
        assertThat(rentAResp.getBody().rentalId()).isNotNull();
    }

    @Test
    void should_add_customer_to_waiting_list_when_bike_unavailable_and_return_202() {
        // When: Customer B tries to rent the same (already rented) bike
        RentBikeRequest rentByB = new RentBikeRequest(bikeId, customerBId, 3);

        ResponseEntity<RentBikeResultResponse> resp = rest.exchange(
                API + "/rentals",
                HttpMethod.POST,
                new HttpEntity<>(rentByB, authJsonHeaders(customerBToken)),
                RentBikeResultResponse.class
        );

        // Then: WAITLISTED with 202 Accepted and a waitingListEntryId
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().result().name()).isEqualTo("WAITLISTED");
        assertThat(resp.getBody().waitingListEntryId())
                .as("waitingListEntryId should be present when WAITLISTED")
                .isNotNull();
        assertThat(resp.getBody().rentalId())
                .as("rentalId should be null when WAITLISTED")
                .isNull();

        log.info("US_06 OK - bikeId={}, customerBId={}, waitingListEntryId={}",
                bikeId, customerBId, resp.getBody().waitingListEntryId());
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
