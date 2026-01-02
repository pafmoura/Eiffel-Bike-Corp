package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.UserType;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserLoginRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserRegisterRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.UserLoginResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.UserResponse;
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
class BikeCatalogControllerTest {

    @Autowired
    TestRestTemplate rest;

    private String accessToken;
    private UUID providerId;

    @Autowired
    private StudentRepository studentRepository;

    @BeforeEach
    void setup() {
        String password = "secret123";
        String renterEmail = "renter+" + UUID.randomUUID() + "@example.com";
        UserResponse userResponse =  registerUser(UserType.STUDENT, "Renter", renterEmail, password);
        accessToken = login(renterEmail, password);
        providerId = userResponse.providerId();
    }

    @Test
    void should_return_400_when_create_request_is_invalid() {
        BikeCreateRequest invalid = new BikeCreateRequest(
                "",                         // @NotBlank
                null,                       // @NotNull
                null,                       // @NotNull
                null                        // @NotNull
        );
        ResponseEntity<String> r = rest.exchange(
                "/api/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(invalid, authJsonHeaders(accessToken)),
                String.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        assertThat(r.getBody()).isNotBlank();
    }

    @Test
    void should_return_401_when_create_request_is_invalid() {
        BikeCreateRequest invalid = new BikeCreateRequest(
                "test",
                ProviderType.STUDENT,
                providerId,
                BigDecimal.valueOf(10)
        );
        ResponseEntity<String> r = rest.exchange(
                "/api/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(invalid, jsonHeaders()),
                String.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        assertThat(r.getBody()).isNotBlank();
    }

    @Test
    void should_return_404_when_create_request_is_invalid() {
        BikeCreateRequest invalid = new BikeCreateRequest(
                "test",
                ProviderType.EMPLOYEE,
                providerId,
                BigDecimal.valueOf(10)
        );
        ResponseEntity<String> r = rest.exchange(
                "/api/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(invalid, authJsonHeaders(accessToken)),
                String.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        assertThat(r.getBody()).isNotBlank();
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
}
