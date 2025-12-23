package fr.eiffelbikecorp.bikeapi.us;

import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.domain.enums.UserType;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.RentBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.ReturnBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserLoginRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserRegisterRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.NotificationResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.RentBikeResultResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.ReturnBikeResponse;
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
class UserStory07Test {
    // US_07: As a Customer, I want to receive a notification when a bike becomes available
    //        so that I can rent it before someone else does.

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    private Long bikeId;

    private String customerAToken;
    private UUID customerAId;
    private Long rentalAId;

    private String customerBToken;
    private UUID customerBId;
    private Long waitingEntryBId;

    @BeforeEach
    void setup() {
        String password = "secret123";

        // 1) Student offers one bike
        String studentEmail = "student+" + UUID.randomUUID() + "@example.com";
        ResponseEntity<UserResponse> studentRegister = rest.exchange(
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
        assertThat(studentRegister.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(studentRegister.getBody()).isNotNull();
        UUID studentProviderId = studentRegister.getBody().providerId();
        assertThat(studentProviderId).isNotNull();

        ResponseEntity<UserLoginResponse> studentLogin = rest.exchange(
                API + "/users/login",
                HttpMethod.POST,
                new HttpEntity<>(new UserLoginRequest(studentEmail, password), jsonHeaders()),
                UserLoginResponse.class
        );
        assertThat(studentLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(studentLogin.getBody()).isNotNull();
        String studentToken = studentLogin.getBody().accessToken();
        assertThat(studentToken).isNotBlank();

        ResponseEntity<BikeResponse> bikeCreate = rest.exchange(
                API + "/bikes",
                HttpMethod.POST,
                new HttpEntity<>(new BikeCreateRequest(
                        "Bike that will trigger notification",
                        ProviderType.STUDENT,
                        studentProviderId,
                        new BigDecimal("2.00")
                ), authJsonHeaders(studentToken)),
                BikeResponse.class
        );
        assertThat(bikeCreate.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bikeCreate.getBody()).isNotNull();
        this.bikeId = bikeCreate.getBody().id();
        assertThat(bikeId).isNotNull();

        // 2) Create Customer A (will rent first)
        var a = registerAndLoginCustomer("Customer A", password);
        this.customerAId = a.customerId;
        this.customerAToken = a.token;

        // 3) Create Customer B (will be waitlisted)
        var b = registerAndLoginCustomer("Customer B", password);
        this.customerBId = b.customerId;
        this.customerBToken = b.token;

        // 4) Customer A rents => bike becomes unavailable
        ResponseEntity<RentBikeResultResponse> rentA = rest.exchange(
                API + "/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(bikeId, customerAId, 2), authJsonHeaders(customerAToken)),
                RentBikeResultResponse.class
        );
        assertThat(rentA.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(rentA.getBody()).isNotNull();
        assertThat(rentA.getBody().result()).isEqualTo(RentResult.RENTED);
        assertThat(rentA.getBody().rentalId()).isNotNull();
        this.rentalAId = rentA.getBody().rentalId();

        // 5) Customer B tries to rent => WAITLISTED
        ResponseEntity<RentBikeResultResponse> rentB = rest.exchange(
                API + "/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(bikeId, customerBId, 3), authJsonHeaders(customerBToken)),
                RentBikeResultResponse.class
        );
        assertThat(rentB.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(rentB.getBody()).isNotNull();
        assertThat(rentB.getBody().result()).isEqualTo(RentResult.WAITLISTED);
        assertThat(rentB.getBody().waitingListEntryId()).isNotNull();
        this.waitingEntryBId = rentB.getBody().waitingListEntryId();
    }

    @Test
    void should_receive_notification_when_bike_becomes_available() {
        // When: Customer A returns the bike
        ReturnBikeRequest returnReq = new ReturnBikeRequest(
                customerAId,
                "Returning the bike in good condition.",
                "OK"
        );

        ResponseEntity<ReturnBikeResponse> returnResp = rest.exchange(
                API + "/rentals/" + rentalAId + "/return",
                HttpMethod.POST,
                new HttpEntity<>(returnReq, authJsonHeaders(customerAToken)),
                ReturnBikeResponse.class
        );

        // Then: return succeeded
        assertThat(returnResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(returnResp.getBody()).isNotNull();
        assertThat(returnResp.getBody().closedRental()).isNotNull();

        // And: a notification was produced for the waiting customer (Customer B)
        assertThat(returnResp.getBody().notificationSent())
                .as("notificationSent should be present when someone is waiting")
                .isNotNull();

        NotificationResponse notif = returnResp.getBody().notificationSent();
        assertThat(notif.customerId()).isEqualTo(customerBId);
        assertThat(notif.bikeId()).isEqualTo(bikeId);
        assertThat(notif.message()).isNotBlank();
        assertThat(notif.sentAt()).isNotNull();

        // And: Customer B can see it in their notification list
        ParameterizedTypeReference<List<NotificationResponse>> listType = new ParameterizedTypeReference<>() {};
        ResponseEntity<List<NotificationResponse>> listResp = rest.exchange(
                API + "/rentals/notifications?customerId=" + customerBId,
                HttpMethod.GET,
                new HttpEntity<>(authJsonHeaders(customerBToken)),
                listType
        );

        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).isNotNull();
        assertThat(listResp.getBody())
                .extracting(NotificationResponse::bikeId)
                .contains(bikeId);

        log.info("US_07 OK - bikeId={}, rentalAId={}, waitingEntryBId={}, notificationId={}",
                bikeId, rentalAId, waitingEntryBId, notif.id());
    }

    private static class LoginResult {
        final UUID customerId;
        final String token;

        private LoginResult(UUID customerId, String token) {
            this.customerId = customerId;
            this.token = token;
        }
    }

    private LoginResult registerAndLoginCustomer(String fullName, String password) {
        String email = fullName.toLowerCase().replace(" ", "") + "+" + UUID.randomUUID() + "@example.com";

        ResponseEntity<UserResponse> register = rest.exchange(
                API + "/users/register",
                HttpMethod.POST,
                new HttpEntity<>(new UserRegisterRequest(
                        UserType.CUSTOMER,
                        fullName,
                        email,
                        password
                ), jsonHeaders()),
                UserResponse.class
        );
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(register.getBody()).isNotNull();
        UUID customerId = register.getBody().customerId();
        assertThat(customerId).isNotNull();

        ResponseEntity<UserLoginResponse> login = rest.exchange(
                API + "/users/login",
                HttpMethod.POST,
                new HttpEntity<>(new UserLoginRequest(email, password), jsonHeaders()),
                UserLoginResponse.class
        );
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).isNotNull();
        String token = login.getBody().accessToken();
        assertThat(token).isNotBlank();

        return new LoginResult(customerId, token);
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
