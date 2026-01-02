package fr.eiffelbikecorp.bikeapi.us;

import fr.eiffelbikecorp.bikeapi.domain.enums.BikeStatus;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.UserType;
import fr.eiffelbikecorp.bikeapi.dto.request.*;
import fr.eiffelbikecorp.bikeapi.dto.response.*;
import lombok.extern.slf4j.Slf4j;
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

@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserStory07Test {
    // US_07: As Student or Employee, 
    // I want to receive a notification when a bike becomes available
    // so that I can rent it before someone else does.

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    private UUID providerId;
    private UUID customerId_1;
    private UUID customerId_2;

    private Long bikeId;

    private String accessToken;
    private String accessToken2;

    void createProviderAndOfferBikeForRent(UserType userType) {
        String studentEmail = UUID.randomUUID() + "@example.com";
        String password = "secret123";
        ResponseEntity<UserResponse> studentRegisterResp = rest.exchange(
                API + "/users/register",
                HttpMethod.POST,
                new HttpEntity<>(new UserRegisterRequest(
                        userType,
                        "Student Provider",
                        studentEmail,
                        password
                ), jsonHeaders()),
                UserResponse.class
        );
        assertThat(studentRegisterResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(studentRegisterResp.getBody()).isNotNull();
        assertThat(studentRegisterResp.getBody().providerId()).isNotNull();
        this.providerId = studentRegisterResp.getBody().providerId();
        ResponseEntity<UserLoginResponse> studentLoginResp = rest.exchange(
                API + "/users/login",
                HttpMethod.POST,
                new HttpEntity<>(new UserLoginRequest(studentEmail, password), jsonHeaders()),
                UserLoginResponse.class
        );
        assertThat(studentLoginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(studentLoginResp.getBody()).isNotNull();
        accessToken = studentLoginResp.getBody().accessToken();
        assertThat(accessToken).isNotBlank();
        ResponseEntity<BikeResponse> bikeCreateResp = rest.exchange(
                API + "/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(new BikeCreateRequest(
                        "Commute bike - available",
                        ProviderType.STUDENT,
                        providerId,
                        new BigDecimal("2.25")
                ), authJsonHeaders(accessToken)),
                BikeResponse.class
        );
        assertThat(bikeCreateResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bikeCreateResp.getBody()).isNotNull();
        this.bikeId = bikeCreateResp.getBody().id();
        assertThat(bikeId).isNotNull();
    }

    void createCustomer_1_ForRent(UserType userType) {
        String customerEmail = UUID.randomUUID() + "@example.com";
        String password = "secret123";
        ResponseEntity<UserResponse> customerRegisterResp = rest.exchange(
                API + "/users/register",
                HttpMethod.POST,
                new HttpEntity<>(new UserRegisterRequest(
                        userType,
                        "Renter 1 " + userType.name(),
                        customerEmail,
                        password
                ), jsonHeaders()),
                UserResponse.class
        );
        assertThat(customerRegisterResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(customerRegisterResp.getBody()).isNotNull();
        this.customerId_1 = customerRegisterResp.getBody().customerId();
        assertThat(customerId_1).isNotNull();
        ResponseEntity<UserLoginResponse> customerLoginResp = rest.exchange(
                API + "/users/login",
                HttpMethod.POST,
                new HttpEntity<>(new UserLoginRequest(customerEmail, password), jsonHeaders()),
                UserLoginResponse.class
        );
        assertThat(customerLoginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(customerLoginResp.getBody()).isNotNull();
        this.accessToken = customerLoginResp.getBody().accessToken();
        assertThat(accessToken).isNotBlank();
    }

    void createCustomer_2_ForRent(UserType userType) {
        String customerEmail = UUID.randomUUID() + "@example.com";
        String password = "secret123";
        ResponseEntity<UserResponse> customerRegisterResp = rest.exchange(
                API + "/users/register",
                HttpMethod.POST,
                new HttpEntity<>(new UserRegisterRequest(
                        userType,
                        "Renter 2" + userType.name(),
                        customerEmail,
                        password
                ), jsonHeaders()),
                UserResponse.class
        );
        assertThat(customerRegisterResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(customerRegisterResp.getBody()).isNotNull();
        this.customerId_2 = customerRegisterResp.getBody().customerId();
        assertThat(customerId_2).isNotNull();
        ResponseEntity<UserLoginResponse> customerLoginResp = rest.exchange(
                API + "/users/login",
                HttpMethod.POST,
                new HttpEntity<>(new UserLoginRequest(customerEmail, password), jsonHeaders()),
                UserLoginResponse.class
        );
        assertThat(customerLoginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(customerLoginResp.getBody()).isNotNull();
        this.accessToken2 = customerLoginResp.getBody().accessToken();
        assertThat(accessToken2).isNotBlank();
    }

    @Test
    void should_see_notification_list_and_return_200() {
        createProviderAndOfferBikeForRent(UserType.STUDENT);
        createCustomer_1_ForRent(UserType.STUDENT);
        createCustomer_2_ForRent(UserType.STUDENT);
        RentBikeRequest rentReq = new RentBikeRequest(bikeId, customerId_1, 3);
        ResponseEntity<RentBikeResultResponse> rentResp = rest.exchange(
                API + "/rentals",
                HttpMethod.POST,
                new HttpEntity<>(rentReq, authJsonHeaders(accessToken)),
                RentBikeResultResponse.class
        );
        assertThat(rentResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(rentResp.getBody()).isNotNull();
        assertThat(rentResp.getBody().result()).isNotNull();
        assertThat(BikeStatus.valueOf(rentResp.getBody().result().toString())).isEqualTo(BikeStatus.RENTED);
        assertThat(rentResp.getBody().rentalId()).as("rentalId should be present when RENTED").isNotNull();
        assertThat(rentResp.getBody().waitingListEntryId()).as("waitingListEntryId should be null when RENTED").isNull();
        RentBikeRequest rentByB = new RentBikeRequest(bikeId, customerId_2, 3);
        ResponseEntity<RentBikeResultResponse> resp = rest.exchange(
                API + "/rentals",
                HttpMethod.POST,
                new HttpEntity<>(rentByB, authJsonHeaders(accessToken2)),
                RentBikeResultResponse.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().result().name()).isEqualTo("WAITLISTED");
        assertThat(resp.getBody().waitingListEntryId())
                .as("waitingListEntryId should be present when WAITLISTED")
                .isNotNull();
        assertThat(resp.getBody().rentalId())
                .as("rentalId should be null when WAITLISTED")
                .isNull();
        // When: Customer A returns the bike
        ReturnBikeRequest returnReq = new ReturnBikeRequest(
                customerId_1,
                "Returning the bike in good condition.",
                "OK"
        );
        ResponseEntity<ReturnBikeResponse> returnResp = rest.exchange(
                API + "/rentals/" + rentResp.getBody().rentalId() + "/return",
                HttpMethod.POST,
                new HttpEntity<>(returnReq, authJsonHeaders(accessToken)),
                ReturnBikeResponse.class
        );
        assertThat(returnResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(returnResp.getBody()).isNotNull();
        assertThat(returnResp.getBody().closedRental()).isNotNull();
        assertThat(returnResp.getBody().notificationSent())
                .as("notificationSent should be present when someone is waiting")
                .isNotNull();
        NotificationResponse notif = returnResp.getBody().notificationSent();
        assertThat(notif.customerId()).isEqualTo(customerId_2);
        assertThat(notif.bikeId()).isEqualTo(bikeId);
        assertThat(notif.message()).isNotBlank();
        assertThat(notif.sentAt()).isNotNull();
        // And: Customer B can see it in their notification list
        ParameterizedTypeReference<List<NotificationResponse>> listType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<List<NotificationResponse>> listResp = rest.exchange(
                API + "/rentals/notifications?customerId=" + customerId_2,
                HttpMethod.GET,
                new HttpEntity<>(authJsonHeaders(accessToken2)),
                listType
        );
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).isNotNull();
        assertThat(listResp.getBody())
                .extracting(NotificationResponse::bikeId)
                .contains(bikeId);
        log.info("US_07 OK - bikeId={}, rentalAId={}, waitingEntryBId={}, notificationId={}",
                bikeId, rentResp.getBody().rentalId(), "", notif.id());
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
