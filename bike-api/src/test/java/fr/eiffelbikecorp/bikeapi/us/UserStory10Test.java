package fr.eiffelbikecorp.bikeapi.us;

import fr.eiffelbikecorp.bikeapi.domain.entity.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.domain.enums.UserType;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.CreateSaleOfferRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.RentBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.ReturnBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserLoginRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserRegisterRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.RentBikeResultResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.ReturnBikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.SaleOfferDetailsResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.SaleOfferResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.UserLoginResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.UserResponse;
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
class UserStory10Test {
    // US_10: As EiffelBikeCorp, I want to list for sale only company bikes that have been rented at least once
    //        so that only used corporate bikes are eligible for resale.

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository corpRepository;

    private UUID corpProviderId;

    private String operatorToken; // token used to call secured endpoints
    private UUID renterCustomerId;
    private String renterToken;

    private Long corpBikeId;

    @BeforeEach
    void setup() {
        String password = "secret123";

        // 0) Ensure corp provider exists
        EiffelBikeCorp corp = corpRepository.save(new EiffelBikeCorp());
        this.corpProviderId = corp.getId();
        assertThat(corpProviderId).isNotNull();

        // 1) Create an authenticated "operator" (any secured user can call /bikes and /sale-offers)
        String operatorEmail = "operator+" + UUID.randomUUID() + "@example.com";
        registerUser(UserType.CUSTOMER, "Corp Operator", operatorEmail, password);
        this.operatorToken = login(operatorEmail, password);

        // 2) Create the renting customer (the rental will create the "rented at least once" history)
        String renterEmail = "renter+" + UUID.randomUUID() + "@example.com";
        UserResponse renter = registerUser(UserType.CUSTOMER, "Renter Customer", renterEmail, password);
        this.renterCustomerId = renter.customerId();
        this.renterToken = login(renterEmail, password);

        // 3) EiffelBikeCorp offers a company bike for rent
        ResponseEntity<BikeResponse> bikeCreateResp = rest.exchange(
                API + "/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(new BikeCreateRequest(
                        "Corp bike - eligible after first rental",
                        ProviderType.EIFFEL_BIKE_CORP,
                        corpProviderId,
                        new BigDecimal("1.50")
                ), authJsonHeaders(operatorToken)),
                BikeResponse.class
        );
        assertThat(bikeCreateResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bikeCreateResp.getBody()).isNotNull();
        this.corpBikeId = bikeCreateResp.getBody().id();
        assertThat(corpBikeId).isNotNull();

        // 4) Rent it once (to satisfy "rented at least once")
        ResponseEntity<RentBikeResultResponse> rentResp = rest.exchange(
                API + "/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(corpBikeId, renterCustomerId, 1), authJsonHeaders(renterToken)),
                RentBikeResultResponse.class
        );
        assertThat(rentResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(rentResp.getBody()).isNotNull();
        assertThat(rentResp.getBody().result()).isEqualTo(RentResult.RENTED);
        assertThat(rentResp.getBody().rentalId()).isNotNull();
        Long rentalId = rentResp.getBody().rentalId();

        // 5) Return it so the bike becomes available again for resale listing
        ResponseEntity<ReturnBikeResponse> returnResp = rest.exchange(
                API + "/rentals/" + rentalId + "/return",
                HttpMethod.POST,
                new HttpEntity<>(new ReturnBikeRequest(
                        renterCustomerId,
                        "Returned after first rental.",
                        "OK"
                ), authJsonHeaders(renterToken)),
                ReturnBikeResponse.class
        );
        assertThat(returnResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(returnResp.getBody()).isNotNull();
        assertThat(returnResp.getBody().closedRental()).isNotNull();
    }

    @Test
    void should_create_sale_offer_only_after_company_bike_has_been_rented_at_least_once() {
        // When: list the USED corporate bike for sale
        CreateSaleOfferRequest req = new CreateSaleOfferRequest(
                corpBikeId,
                corpProviderId,
                new BigDecimal("120.00")
        );

        ResponseEntity<SaleOfferResponse> saleResp = rest.exchange(
                API + "/sale-offers",
                HttpMethod.POST,
                new HttpEntity<>(req, authJsonHeaders(operatorToken)),
                SaleOfferResponse.class
        );

        // Then: sale offer is created and listed
        assertThat(saleResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(saleResp.getBody()).isNotNull();
        assertThat(saleResp.getBody().id()).isNotNull();
        assertThat(saleResp.getBody().bikeId()).isEqualTo(corpBikeId);
        assertThat(String.valueOf(saleResp.getBody().status())).isEqualTo("LISTED");
        assertThat(saleResp.getBody().askingPriceEur()).isEqualByComparingTo("120.00");

        Long saleOfferId = saleResp.getBody().id();

        // And: details endpoint by bike returns the same offer
        ResponseEntity<SaleOfferDetailsResponse> detailsResp = rest.exchange(
                API + "/sale-offers/by-bike/" + corpBikeId,
                HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()),
                SaleOfferDetailsResponse.class
        );

        assertThat(detailsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailsResp.getBody()).isNotNull();
        assertThat(detailsResp.getBody().offer()).isNotNull();
        assertThat(detailsResp.getBody().offer().id()).isEqualTo(saleOfferId);
        assertThat(detailsResp.getBody().offer().bikeId()).isEqualTo(corpBikeId);

        log.info("US_10 OK - corpProviderId={}, bikeId={}, saleOfferId={}", corpProviderId, corpBikeId, saleOfferId);
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
