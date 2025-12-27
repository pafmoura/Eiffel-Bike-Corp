package fr.eiffelbikecorp.bikeapi.us;

import fr.eiffelbikecorp.bikeapi.domain.entity.EiffelBikeCorp;
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
class UserStory13Test {
    // US_13: As a Customer, I want to view the sale price of bikes offered for sale
    //        so that I can compare options before buying.

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository corpRepository;

    private UUID corpProviderId;
    private String operatorToken;

    private Long saleOfferId;
    private Long bikeId;

    @BeforeEach
    void setup() {
        String password = "secret123";

        // Ensure corp provider exists
        EiffelBikeCorp corp = corpRepository.save(new EiffelBikeCorp());
        this.corpProviderId = corp.getId();
        assertThat(corpProviderId).isNotNull();

        // Operator to create bike + sale offer
        String operatorEmail = "operator+" + UUID.randomUUID() + "@example.com";
        registerUser(UserType.CUSTOMER, "Corp Operator", operatorEmail, password);
        this.operatorToken = login(operatorEmail, password);

        // Renter to rent once + return (compat with US_10 rule)
        String renterEmail = "renter+" + UUID.randomUUID() + "@example.com";
        UserResponse renter = registerUser(UserType.CUSTOMER, "Renter", renterEmail, password);
        String renterToken = login(renterEmail, password);

        // Create corp bike
        ResponseEntity<BikeResponse> bikeCreate = rest.exchange(
                API + "/bikes",
                HttpMethod.POST,
                new HttpEntity<>(new BikeCreateRequest(
                        "Sale bike - compare price",
                        ProviderType.EIFFEL_BIKE_CORP,
                        corpProviderId,
                        new BigDecimal("1.50")
                ), authJsonHeaders(operatorToken)),
                BikeResponse.class
        );
        assertThat(bikeCreate.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bikeCreate.getBody()).isNotNull();
        this.bikeId = bikeCreate.getBody().id();
        assertThat(bikeId).isNotNull();

        // Rent once + return
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

        // Create sale offer with a known price
        ResponseEntity<SaleOfferResponse> offer = rest.exchange(
                API + "/sale-offers",
                HttpMethod.POST,
                new HttpEntity<>(new CreateSaleOfferRequest(
                        bikeId,
                        corpProviderId,
                        new BigDecimal("199.99")
                ), authJsonHeaders(operatorToken)),
                SaleOfferResponse.class
        );
        assertThat(offer.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(offer.getBody()).isNotNull();
        this.saleOfferId = offer.getBody().id();
        assertThat(saleOfferId).isNotNull();
    }

    @Test
    void should_view_sale_price_in_offer_list_and_details() {
        // When: customer lists sale offers (public endpoint)
        ParameterizedTypeReference<List<SaleOfferResponse>> type = new ParameterizedTypeReference<>() {};
        ResponseEntity<List<SaleOfferResponse>> listResp = rest.exchange(
                API + "/sale-offers?q=Sale",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()),
                type
        );

        // Then: offer appears with price
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).isNotNull();

        SaleOfferResponse listed = listResp.getBody().stream()
                .filter(o -> o.id().equals(saleOfferId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected offer " + saleOfferId + " in list"));

        assertThat(listed.askingPriceEur()).isEqualByComparingTo("199.99");

        // And: details endpoint also shows the same price
        ResponseEntity<SaleOfferDetailsResponse> detailsResp = rest.exchange(
                API + "/sale-offers/" + saleOfferId,
                HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()),
                SaleOfferDetailsResponse.class
        );

        assertThat(detailsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailsResp.getBody()).isNotNull();
        assertThat(detailsResp.getBody().offer()).isNotNull();
        assertThat(detailsResp.getBody().offer().id()).isEqualTo(saleOfferId);
        assertThat(detailsResp.getBody().offer().askingPriceEur()).isEqualByComparingTo("199.99");

        log.info("US_13 OK - saleOfferId={}, bikeId={}, priceEur={}", saleOfferId, bikeId, listed.askingPriceEur());
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
