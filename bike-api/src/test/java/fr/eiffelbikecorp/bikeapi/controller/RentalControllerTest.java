package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.enums.UserType;
import fr.eiffelbikecorp.bikeapi.dto.request.RentBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserLoginRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserRegisterRequest;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RentalControllerTest {

    @Autowired
    TestRestTemplate rest;

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
    void should_return_400_when_rent_request_is_invalid() {
        RentBikeRequest invalid = new RentBikeRequest(null, null, null);
        ResponseEntity<String> r = rest.exchange(
                "/api/rentals",
                HttpMethod.POST,
                new HttpEntity<>(invalid, authJsonHeaders(customerToken)),
                String.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        assertThat(r.getBody()).isNotBlank();
    }
}
