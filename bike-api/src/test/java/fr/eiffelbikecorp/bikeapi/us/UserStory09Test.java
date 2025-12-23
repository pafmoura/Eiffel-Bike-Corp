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
import fr.eiffelbikecorp.bikeapi.dto.response.RentBikeResultResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.ReturnBikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.UserLoginResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.UserResponse;
import fr.eiffelbikecorp.bikeapi.domain.entity.ReturnNote;
import fr.eiffelbikecorp.bikeapi.persistence.ReturnNoteRepository;
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
class UserStory09Test {
    // US_09: As a Customer, I want to add notes when returning a bike so that the next renter
    //        and the bike provider know the bike’s condition.

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    @Autowired
    ReturnNoteRepository returnNoteRepository;

    private String studentToken;
    private UUID studentProviderId;
    private Long bikeId;

    private String customerToken;
    private UUID customerId;

    private Long rentalId;

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
        this.studentProviderId = studentRegisterResp.getBody().providerId();
        assertThat(studentProviderId).isNotNull();

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
                API + "/bikes",
                HttpMethod.POST,
                new HttpEntity<>(new BikeCreateRequest(
                        "Bike for return notes",
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

        // 2) Customer registers + logs in
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

        // 3) Customer rents the bike
        ResponseEntity<RentBikeResultResponse> rentResp = rest.exchange(
                API + "/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(bikeId, customerId, 1), authJsonHeaders(customerToken)),
                RentBikeResultResponse.class
        );

        assertThat(rentResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(rentResp.getBody()).isNotNull();
        assertThat(rentResp.getBody().result()).isEqualTo(RentResult.RENTED);
        assertThat(rentResp.getBody().rentalId()).isNotNull();
        this.rentalId = rentResp.getBody().rentalId();
    }

    @Test
    void should_add_return_note_when_returning_bike() {
        // Given: return note content
        String comment = "Brakes are slightly squeaky. Otherwise OK.";
        String condition = "MINOR_ISSUES";

        ReturnBikeRequest returnReq = new ReturnBikeRequest(
                customerId,
                comment,
                condition
        );

        // When: customer returns the bike with notes
        ResponseEntity<ReturnBikeResponse> returnResp = rest.exchange(
                API + "/rentals/" + rentalId + "/return",
                HttpMethod.POST,
                new HttpEntity<>(returnReq, authJsonHeaders(customerToken)),
                ReturnBikeResponse.class
        );

        // Then: return is OK
        assertThat(returnResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(returnResp.getBody()).isNotNull();
        assertThat(returnResp.getBody().closedRental()).isNotNull();

        // And: the note is persisted (so provider/next renter can see condition)
        // (We verify at persistence level since no "get return note" endpoint was shown in controllers.)
        ReturnNote note = returnNoteRepository.findAll().stream()
                .filter(n -> n.getRental() != null && n.getRental().getId().equals(rentalId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected a ReturnNote for rental " + rentalId));

        assertThat(note.getAuthor()).isNotNull();
        assertThat(note.getAuthor().getId()).isEqualTo(customerId);
        assertThat(note.getComment()).isEqualTo(comment);
        assertThat(note.getCondition()).isEqualTo(condition);
        assertThat(note.getCreatedAt()).isNotNull();

        log.info("US_09 OK - rentalId={}, returnNoteId={}, authorCustomerId={}",
                rentalId, note.getId(), customerId);
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
