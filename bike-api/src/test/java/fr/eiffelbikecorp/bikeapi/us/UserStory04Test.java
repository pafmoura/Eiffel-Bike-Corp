package fr.eiffelbikecorp.bikeapi.us;

import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.UserType;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserLoginRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserRegisterRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.BikeResponse;
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
class UserStory04Test {
    //* **US_04:** As Student or Employee,
    // I want to search for bikes available for rent
    // so that I can quickly find a suitable bike.

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    private String customerToken;

    // Test data (bikes offered by a student)
    private UUID studentProviderId;
    private Long bikeId1;
    private Long bikeId2;

    @BeforeEach
    void setup() {
        String studentEmail = "student+" + UUID.randomUUID() + "@example.com";
        String password = "secret123";
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
        String studentToken = studentLoginResp.getBody().accessToken();
        assertThat(studentToken).isNotBlank();
        bikeId1 = createBike(studentToken,
                new BikeCreateRequest("City bike - blue", ProviderType.STUDENT, studentProviderId, new BigDecimal("2.50"))
        );
        bikeId2 = createBike(studentToken,
                new BikeCreateRequest("Mountain bike - red", ProviderType.STUDENT, studentProviderId, new BigDecimal("3.00"))
        );
        String customerEmail = "customer+" + UUID.randomUUID() + "@example.com";
        ResponseEntity<UserResponse> customerRegisterResp = rest.exchange(
                API + "/users/register",
                HttpMethod.POST,
                new HttpEntity<>(new UserRegisterRequest(
                        UserType.STUDENT,
                        "Customer One",
                        customerEmail,
                        password
                ), jsonHeaders()),
                UserResponse.class
        );
        assertThat(customerRegisterResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
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
    void should_search_available_bikes_and_return_200() {
        ParameterizedTypeReference<List<BikeResponse>> type = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<List<BikeResponse>> resp = rest.exchange(
                API + "/bikes?status=AVAILABLE",
                HttpMethod.GET,
                new HttpEntity<>(authJsonHeaders(customerToken)),
                type
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType()).isNotNull();
        assertThat(resp.getHeaders().getContentType().toString()).contains("application/json");
        assertThat(resp.getBody()).isNotNull();
        List<BikeResponse> bikes = resp.getBody();
        assertThat(bikes)
                .extracting(BikeResponse::id)
                .contains(bikeId1, bikeId2);
        assertThat(bikes)
                .filteredOn(b -> b.id().equals(bikeId1) || b.id().equals(bikeId2))
                .allSatisfy(b -> assertThat(b.status()).isEqualTo("AVAILABLE"));
        log.info("US_04 OK - found bikes {} and {} in AVAILABLE search", bikeId1, bikeId2);
    }

    private Long createBike(String token, BikeCreateRequest req) {
        ResponseEntity<BikeResponse> resp = rest.exchange(
                API + "/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(req, authJsonHeaders(token)),
                BikeResponse.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().id()).isNotNull();
        return resp.getBody().id();
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
