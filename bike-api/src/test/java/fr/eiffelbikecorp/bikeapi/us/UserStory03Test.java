package fr.eiffelbikecorp.bikeapi.us;

import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.UserType;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserLoginRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserRegisterRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.UserLoginResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.UserResponse;
import fr.eiffelbikecorp.bikeapi.domain.entity.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
import fr.eiffelbikecorp.bikeapi.security.TokenService;
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
class UserStory03Test {
    //* **US_03:** As EiffelBikeCorp, I want to offer company bikes for rent so that customers can rent bikes even when no private bikes are available.

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository corpRepository;

    private String accessToken;
    private UUID corpProviderId;
    @Autowired
    private TokenService tokenService;

    @BeforeEach
    void setup() {
        // Ensure a corp provider exists
        EiffelBikeCorp corp = corpRepository.save(new EiffelBikeCorp());
        this.corpProviderId = corp.getId();
        assertThat(corpProviderId).isNotNull();
        // Create an authenticated user (any secured user can call /bikes; corp "actor" is represented by ProviderType + providerId)
        String email = "corp-operator+" + UUID.randomUUID() + "@example.com";
        String password = "secret123";
        var registerReq = new UserRegisterRequest(
                UserType.EIFFEL_BIKE_CORP,
                "Corp Operator",
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
    void should_offer_company_bike_for_rent_and_return_201() {
        // Given: EiffelBikeCorp provider exists and caller is authenticated
        BikeCreateRequest bikeReq = new BikeCreateRequest(
                "Company city bike - fleet #1",
                ProviderType.EIFFEL_BIKE_CORP,
                corpProviderId,
                new BigDecimal("1.75")
        );
        //acess token
        // When: company offers a bike for rent
        ResponseEntity<BikeResponse> createResp = rest.exchange(
                API + "/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(bikeReq, authJsonHeaders(accessToken)),
                BikeResponse.class
        );
        // Then: bike is created and listed as AVAILABLE
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResp.getBody()).isNotNull();
        assertThat(createResp.getBody().id()).isNotNull();
        assertThat(createResp.getBody().description()).isEqualTo("Company city bike - fleet #1");
        assertThat(createResp.getBody().status()).isEqualTo("AVAILABLE");
        assertThat(createResp.getBody().rentalDailyRateEur()).isEqualByComparingTo("1.75");
        assertThat(createResp.getBody().offeredBy()).isNotNull();
        Long createdBikeId = createResp.getBody().id();
        // And: it is visible in the available list (so customers can rent it)
        ParameterizedTypeReference<List<BikeResponse>> listType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<List<BikeResponse>> listResp = rest.exchange(
                API + "/bikes?status=AVAILABLE&offeredById=" + corpProviderId,
                HttpMethod.GET,
                new HttpEntity<>(authJsonHeaders(accessToken)),
                listType
        );
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).isNotNull();
        assertThat(listResp.getBody())
                .extracting(BikeResponse::id)
                .contains(createdBikeId);
        log.info("US_03 OK - corpProviderId={}, bikeId={}", corpProviderId, createdBikeId);
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
