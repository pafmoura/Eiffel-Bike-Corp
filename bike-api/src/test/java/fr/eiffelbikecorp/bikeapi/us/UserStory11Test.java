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

@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserStory11Test {
    // US_11: As EiffelBikeCorp, 
    // I want to offer bikes for sale with detailed notes
    //  so that buyers can assess each bike’s condition before purchasing.

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository corpRepository;

    private UUID corpProviderId;

    private String providerToken;
    private String renterToken;
    private UUID renterId;

    private Long corpBikeId;
    private Long saleOfferId;

    @BeforeEach
    void setup() {
        String password = "secret123";
        String operatorEmail = "operator+" + UUID.randomUUID() + "@example.com";
        UserResponse operator = registerUser(UserType.EIFFEL_BIKE_CORP, "Corp Operator", operatorEmail, password);
        this.providerToken = login(operatorEmail, password);
        this.corpProviderId = operator.providerId();
        String renterEmail = "renter+" + UUID.randomUUID() + "@example.com";
        UserResponse renter = registerUser(UserType.STUDENT, "Renter Customer", renterEmail, password);
        this.renterId = renter.customerId();
        this.renterToken = login(renterEmail, password);
        ResponseEntity<BikeResponse> bikeCreateResp = rest.exchange(
                API + "/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(new BikeCreateRequest(
                        "Corp bike for sale with notes",
                        ProviderType.EIFFEL_BIKE_CORP,
                        corpProviderId,
                        new BigDecimal("1.80")
                ), authJsonHeaders(providerToken)),
                BikeResponse.class
        );
        assertThat(bikeCreateResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bikeCreateResp.getBody()).isNotNull();
        this.corpBikeId = bikeCreateResp.getBody().id();
        assertThat(corpBikeId).isNotNull();
        ResponseEntity<RentBikeResultResponse> rentResp = rest.exchange(
                API + "/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(corpBikeId, renterId, 1), authJsonHeaders(renterToken)),
                RentBikeResultResponse.class
        );
        assertThat(rentResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(rentResp.getBody()).isNotNull();
        assertThat(rentResp.getBody().result()).isEqualTo(RentResult.RENTED);
        assertThat(rentResp.getBody().rentalId()).isNotNull();
        Long rentalId = rentResp.getBody().rentalId();
        ResponseEntity<ReturnBikeResponse> returnResp = rest.exchange(
                API + "/rentals/" + rentalId + "/return",
                HttpMethod.POST,
                new HttpEntity<>(new ReturnBikeRequest(
                        renterId,
                        "Returned after short use.",
                        "OK"
                ), authJsonHeaders(renterToken)),
                ReturnBikeResponse.class
        );
        assertThat(returnResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(returnResp.getBody()).isNotNull();
        assertThat(returnResp.getBody().closedRental()).isNotNull();
        
        ResponseEntity<SaleOfferResponse> saleResp = rest.exchange(
                API + "/sale-offers",
                HttpMethod.POST,
                new HttpEntity<>(new CreateSaleOfferRequest(
                        corpBikeId,
                        corpProviderId,
                        new BigDecimal("149.90")
                ), authJsonHeaders(providerToken)),
                SaleOfferResponse.class
        );
        assertThat(saleResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(saleResp.getBody()).isNotNull();
        assertThat(saleResp.getBody().id()).isNotNull();
        this.saleOfferId = saleResp.getBody().id();
    }

    @Test
    void should_create_sale_offer_with_notes_and_expose_them_in_details() {
        CreateSaleNoteRequest noteReq = new CreateSaleNoteRequest(
                saleOfferId,
                "Condition report",
                "Frame is good. Brakes were adjusted. Tires at ~70%. Small scratch on the fork.",
                "Mechanic A"
        );
        ResponseEntity<SaleNoteResponse> noteResp = rest.exchange(
                API + "/sale-offers/notes",
                HttpMethod.POST,
                new HttpEntity<>(noteReq, authJsonHeaders(providerToken)),
                SaleNoteResponse.class
        );
        assertThat(noteResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(noteResp.getBody()).isNotNull();
        assertThat(noteResp.getBody().id()).isNotNull();
        assertThat(noteResp.getBody().title()).isEqualTo("Condition report");
        ResponseEntity<SaleOfferDetailsResponse> detailsResp = rest.exchange(
                API + "/sale-offers/" + saleOfferId,
                HttpMethod.GET,
                new HttpEntity<>(authJsonHeaders(renterToken)),
                SaleOfferDetailsResponse.class
        );
        assertThat(detailsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailsResp.getBody()).isNotNull();
        assertThat(detailsResp.getBody().offer()).isNotNull();
        assertThat(detailsResp.getBody().offer().id()).isEqualTo(saleOfferId);
        assertThat(detailsResp.getBody().offer().bikeId()).isEqualTo(corpBikeId);
        assertThat(detailsResp.getBody().notes()).isNotNull();
        assertThat(detailsResp.getBody().notes())
                .extracting(SaleNoteResponse::title)
                .contains("Condition report");
        assertThat(detailsResp.getBody().notes())
                .anySatisfy(n -> {
                    assertThat(n.title()).isEqualTo("Condition report");
                    assertThat(n.content()).contains("Frame is good");
                    assertThat(n.createdBy()).isEqualTo("Mechanic A");
                    assertThat(n.createdAt()).isNotNull();
                });
        log.info("US_11 OK - corpProviderId={}, bikeId={}, saleOfferId={}, noteId={}",
                corpProviderId, corpBikeId, saleOfferId, noteResp.getBody().id());
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
