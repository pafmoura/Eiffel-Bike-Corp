package fr.eiffelbikecorp.bikeapi.us;

import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.domain.enums.UserType;
import fr.eiffelbikecorp.bikeapi.dto.request.*;
import fr.eiffelbikecorp.bikeapi.dto.response.*;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
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
class UserStory15Test {
    // US_15: As a Customer, 
    // I want to check whether a bike offered for sale is still available
    // so that I don’t try to buy a sold or unavailable bike.

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository corpRepository;

    private UUID corpProviderId;
    private String operatorToken;

    private Long bikeId;
    private Long saleOfferId;

    @BeforeEach
    void setup() {
        String password = "secret123";
        String operatorEmail = "operator+" + UUID.randomUUID() + "@example.com";
        UserResponse provider = registerUser(UserType.EIFFEL_BIKE_CORP, "Corp Operator", operatorEmail, password);
        this.operatorToken = login(operatorEmail, password);
        this.corpProviderId = provider.providerId();
        String renterEmail = "renter+" + UUID.randomUUID() + "@example.com";
        UserResponse renter = registerUser(UserType.EMPLOYEE, "Renter", renterEmail, password);
        String renterToken = login(renterEmail, password);
        ResponseEntity<BikeResponse> bikeCreate = rest.exchange(
                API + "/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(new BikeCreateRequest(
                        "Corp bike - availability check",
                        ProviderType.EIFFEL_BIKE_CORP,
                        corpProviderId,
                        new BigDecimal("1.25")
                ), authJsonHeaders(operatorToken)),
                BikeResponse.class
        );
        assertThat(bikeCreate.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bikeCreate.getBody()).isNotNull();
        this.bikeId = bikeCreate.getBody().id();
        assertThat(bikeId).isNotNull();
        ResponseEntity<RentBikeResultResponse> rent = rest.exchange(
                API + "/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(bikeId, renter.customerId(), 1), authJsonHeaders(renterToken)),
                RentBikeResultResponse.class
        );
        assertThat(rent.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(rent.getBody()).isNotNull();
        assertThat(rent.getBody().result()).isEqualTo(RentResult.RENTED);
        Long rentalId = rent.getBody().rentalId();
        assertThat(rentalId).isNotNull();
        ResponseEntity<ReturnBikeResponse> returned = rest.exchange(
                API + "/rentals/" + rentalId + "/return",
                HttpMethod.POST,
                new HttpEntity<>(new ReturnBikeRequest(renter.customerId(), "Returned.", "OK"), authJsonHeaders(renterToken)),
                ReturnBikeResponse.class
        );
        assertThat(returned.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<SaleOfferResponse> offer = rest.exchange(
                API + "/sale-offers",
                HttpMethod.POST,
                new HttpEntity<>(new CreateSaleOfferRequest(
                        bikeId,
                        corpProviderId,
                        new BigDecimal("150.00")
                ), authJsonHeaders(operatorToken)),
                SaleOfferResponse.class
        );
        assertThat(offer.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(offer.getBody()).isNotNull();
        this.saleOfferId = offer.getBody().id();
        assertThat(saleOfferId).isNotNull();
    }

    @Test
    void should_check_sale_offer_availability_and_return_200() {
        ResponseEntity<SaleOfferDetailsResponse> detailsResp = rest.exchange(
                API + "/sale-offers/" + saleOfferId,
                HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()),
                SaleOfferDetailsResponse.class
        );
        assertThat(detailsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailsResp.getBody()).isNotNull();
        assertThat(detailsResp.getBody().offer()).isNotNull();
        SaleOfferResponse offer = detailsResp.getBody().offer();
        assertThat(offer.id()).isEqualTo(saleOfferId);
        assertThat(offer.bikeId()).isEqualTo(bikeId);
        assertThat(String.valueOf(offer.status())).isEqualTo("LISTED");
        assertThat(offer.availability()).isNotBlank();
        log.info("US_15 OK - saleOfferId={}, bikeId={}, status={}, availability={}",
                saleOfferId, bikeId, offer.status(), offer.availability());
    }

    private UserResponse registerUser(UserType type, String fullName, String email, String password) {
        ResponseEntity<UserResponse> resp = rest.exchange(
                API + "/users/register",
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
                API + "/users/login",
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
