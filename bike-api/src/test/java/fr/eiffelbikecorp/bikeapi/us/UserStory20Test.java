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
class UserStory20Test {
    // US_20: As a Customer,
    // I want to view my purchase history
    // so that I can track what I bought and when.

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository corpRepository;

    private UUID corpProviderId;

    private String operatorToken;

    private String buyerToken;
    private Long saleOfferId;
    private Long purchaseId;

    @BeforeEach
    void setup() {
        String password = "secret123";
        String operatorEmail = "operator+" + UUID.randomUUID() + "@example.com";
        UserResponse provider = registerUser(UserType.EIFFEL_BIKE_CORP, "Corp Operator", operatorEmail, password);
        operatorToken = login(operatorEmail, password);
        corpProviderId = provider.providerId();
        String renterEmail = "renter+" + UUID.randomUUID() + "@example.com";
        UserResponse renter = registerUser(UserType.STUDENT, "Renter", renterEmail, password);
        String renterToken = login(renterEmail, password);
        ResponseEntity<BikeResponse> bikeCreate = rest.exchange(
                API + "/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(new BikeCreateRequest(
                        "Corp bike - purchase history",
                        ProviderType.EIFFEL_BIKE_CORP,
                        corpProviderId,
                        new BigDecimal("1.50")
                ), authJsonHeaders(operatorToken)),
                BikeResponse.class
        );
        assertThat(bikeCreate.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bikeCreate.getBody()).isNotNull();
        Long bikeId = bikeCreate.getBody().id();
        assertThat(bikeId).isNotNull();
        ResponseEntity<RentBikeResultResponse> rentResp = rest.exchange(
                API + "/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(bikeId, renter.customerId(), 1), authJsonHeaders(renterToken)),
                RentBikeResultResponse.class
        );
        assertThat(rentResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(rentResp.getBody()).isNotNull();
        assertThat(rentResp.getBody().result()).isEqualTo(RentResult.RENTED);
        Long rentalId = rentResp.getBody().rentalId();
        assertThat(rentalId).isNotNull();
        ResponseEntity<ReturnBikeResponse> returnResp = rest.exchange(
                API + "/rentals/" + rentalId + "/return",
                HttpMethod.POST,
                new HttpEntity<>(new ReturnBikeRequest(renter.customerId(), "Returned.", "OK"), authJsonHeaders(renterToken)),
                ReturnBikeResponse.class
        );
        assertThat(returnResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<SaleOfferResponse> offerResp = rest.exchange(
                API + "/sale-offers",
                HttpMethod.POST,
                new HttpEntity<>(new CreateSaleOfferRequest(
                        bikeId,
                        corpProviderId,
                        new BigDecimal("199.00")
                ), authJsonHeaders(operatorToken)),
                SaleOfferResponse.class
        );
        assertThat(offerResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(offerResp.getBody()).isNotNull();
        saleOfferId = offerResp.getBody().id();
        assertThat(saleOfferId).isNotNull();
        String buyerEmail = "buyer+" + UUID.randomUUID() + "@example.com";
        registerUser(UserType.CUSTOMER, "Buyer Customer", buyerEmail, password);
        buyerToken = login(buyerEmail, password);
        ResponseEntity<BasketResponse> basketResp = rest.exchange(
                API + "/basket/items",
                HttpMethod.POST,
                new HttpEntity<>(new AddToBasketRequest(saleOfferId), authJsonHeaders(buyerToken)),
                BasketResponse.class
        );
        assertThat(basketResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(basketResp.getBody()).isNotNull();
        assertThat(basketResp.getBody().items())
                .extracting(BasketItemResponse::saleOfferId)
                .contains(saleOfferId);
        ResponseEntity<PurchaseResponse> checkoutResp = rest.exchange(
                API + "/purchases/checkout",
                HttpMethod.POST,
                new HttpEntity<>(authJsonHeaders(buyerToken)),
                PurchaseResponse.class
        );
        assertThat(checkoutResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(checkoutResp.getBody()).isNotNull();
        purchaseId = checkoutResp.getBody().id();
        assertThat(purchaseId).isNotNull();
        BigDecimal total = checkoutResp.getBody().totalAmountEur();
        assertThat(total).isNotNull();
        ResponseEntity<SalePaymentResponse> payResp = rest.exchange(
                API + "/payments/purchases",
                HttpMethod.POST,
                new HttpEntity<>(new PayPurchaseRequest(
                        purchaseId,
                        total,
                        "EUR",
                        "pm_card_visa"
                ), authJsonHeaders(buyerToken)),
                SalePaymentResponse.class
        );
        assertThat(payResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(payResp.getBody()).isNotNull();
        assertThat(payResp.getBody().purchaseId()).isEqualTo(purchaseId);
        assertThat(payResp.getBody().status()).isEqualTo("PAID");
    }

    @Test
    void should_list_purchase_history_and_include_created_at_and_items() {
        ParameterizedTypeReference<List<PurchaseResponse>> type = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<List<PurchaseResponse>> listResp = rest.exchange(
                API + "/purchases",
                HttpMethod.GET,
                new HttpEntity<>(authJsonHeaders(buyerToken)),
                type
        );
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getHeaders().getContentType()).isNotNull();
        assertThat(listResp.getHeaders().getContentType().toString()).contains("application/json");
        List<PurchaseResponse> purchases = listResp.getBody();
        assertThat(purchases).isNotNull();
        assertThat(purchases).isNotEmpty();
        PurchaseResponse mine = purchases.stream()
                .filter(p -> p.id().equals(purchaseId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected purchase " + purchaseId + " in purchase history"));
        assertThat(mine.items()).isNotNull();
        assertThat(mine.items()).isNotEmpty();
        assertThat(mine.items())
                .extracting(PurchaseItemResponse::saleOfferId)
                .contains(saleOfferId);
        assertThat(mine.createdAt()).isNotNull();
        assertThat(mine.status()).isEqualTo("PAID");
        assertThat(mine.paidAt()).isNotNull();
        log.info("US_20 OK - purchaseId={}, createdAt={}, itemsCount={}",
                mine.id(), mine.createdAt(), mine.items().size());
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
