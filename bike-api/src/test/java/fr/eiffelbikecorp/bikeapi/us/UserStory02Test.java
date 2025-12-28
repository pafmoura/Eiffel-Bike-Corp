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
class UserStory02Test {
    //* **US_02:** As an Employee, I want to offer my bike for rent so that other university members can rent it.

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    private String accessToken;
    private UUID employeeProviderId;
    private UUID employeeCustomerId;

    @BeforeEach
    void setup() {
        String email = "employee+" + UUID.randomUUID() + "@example.com";
        String password = "secret123"; // >= 6
        // 1) Register employee
        var registerReq = new UserRegisterRequest(
                UserType.EMPLOYEE,
                "Employee One",
                email,
                password
        );
        ResponseEntity<UserResponse> registerResp = rest.exchange(
                API + "/users/register",
                HttpMethod.POST,
                new HttpEntity<>(registerReq, jsonHeaders()),
                UserResponse.class
        );
        assertThat(registerResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResp.getBody()).isNotNull();
        assertThat(registerResp.getBody().providerId()).as("employee providerId").isNotNull();
        this.employeeProviderId = registerResp.getBody().providerId();
        this.employeeCustomerId = registerResp.getBody().customerId();
        // 2) Login
        var loginReq = new UserLoginRequest(email, password);
        ResponseEntity<UserLoginResponse> loginResp = rest.exchange(
                API + "/users/login",
                HttpMethod.POST,
                new HttpEntity<>(loginReq, jsonHeaders()),
                UserLoginResponse.class
        );
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResp.getBody()).isNotNull();
        assertThat(loginResp.getBody().accessToken()).isNotBlank();
        this.accessToken = loginResp.getBody().accessToken();
    }

    @Test
    void should_offer_bike_for_rent_and_return_201() {
        // Given: employee is authenticated (setup) and has a providerId
        BikeCreateRequest bikeReq = new BikeCreateRequest(
                "Hybrid bike - employee owned",
                ProviderType.EMPLOYEE,
                employeeProviderId,
                new BigDecimal("3.00")
        );
        // When: employee offers a bike for rent
        ResponseEntity<BikeResponse> createResp = rest.exchange(
                API + "/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(bikeReq, authJsonHeaders(accessToken)),
                BikeResponse.class
        );
        // Then: bike is created
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResp.getBody()).isNotNull();
        assertThat(createResp.getBody().id()).isNotNull();
        assertThat(createResp.getBody().description()).isEqualTo("Hybrid bike - employee owned");
        assertThat(createResp.getBody().status()).isEqualTo("AVAILABLE");
        assertThat(createResp.getBody().rentalDailyRateEur()).isEqualByComparingTo("3.00");
        assertThat(createResp.getBody().offeredBy()).isNotNull();
        Long createdBikeId = createResp.getBody().id();
        // And: bike appears in list (available for others to rent later)
        ParameterizedTypeReference<List<BikeResponse>> listType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<List<BikeResponse>> listResp = rest.exchange(
                API + "/bikes?status=AVAILABLE&offeredById=" + employeeProviderId,
                HttpMethod.GET,
                new HttpEntity<>(authJsonHeaders(accessToken)),
                listType
        );
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).isNotNull();
        assertThat(listResp.getBody())
                .extracting(BikeResponse::id)
                .contains(createdBikeId);
        log.info("US_02 OK - employeeCustomerId={}, employeeProviderId={}, bikeId={}",
                employeeCustomerId, employeeProviderId, createdBikeId);
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
