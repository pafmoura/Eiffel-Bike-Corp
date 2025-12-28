package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.entity.Customer;
import fr.eiffelbikecorp.bikeapi.domain.entity.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.PayRentalRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.RentBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.RentBikeResultResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.RentalPaymentResponse;
import fr.eiffelbikecorp.bikeapi.payment.PaymentGateway;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
import fr.eiffelbikecorp.bikeapi.security.TokenService;
import fr.eiffelbikecorp.bikeapi.service.FxRateService;
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
import org.springframework.orm.jpa.EntityManagerFactoryAccessor;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static fr.eiffelbikecorp.bikeapi.Utils.randomEmail;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(MockitoExtension.class)
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RentalPaymentControllerTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository corpRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    PaymentGateway paymentGateway;
    @Autowired
    FxRateService fxRateService;

    @Autowired
    private TokenService tokenService;
    private String accessToken;

    private UUID corpId = UUID.randomUUID();
    private UUID customerId = UUID.randomUUID();
    @Autowired
    private EntityManagerFactoryAccessor entityManagerFactoryAccessor;

    @BeforeEach
    void setup() {
        EiffelBikeCorp corp = corpRepository.findById(corpId).orElse(null);
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (corp == null) {
            corp = new EiffelBikeCorp();
            corpId = corpRepository.saveAndFlush(corp).getId();
        }
        if (customer == null) {
            Customer c = new Customer();
            c.setEmail(randomEmail());
            c.setFullName("Payment Tester");
            c.setPassword("testpassword");
            c = customerRepository.saveAndFlush(c);
            customerId = c.getId();
            accessToken = tokenService.generateToken(c);
        }
    }

    @Test
    void should_pay_rental_and_return_201_with_response_body() {
        // Arrange: create bike + rent
        BikeResponse bike = createBike(new BikeCreateRequest(
                "Bike for payment test",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.50")
        ));
        Long rentalId = rentBikeAndGetRentalId(bike.id(), customerId, 2);
        PayRentalRequest payReq = new PayRentalRequest(
                rentalId,
                new BigDecimal("10.00"),
                "USD",
                "pm_card_visa"
        );
        // Act
        ResponseEntity<RentalPaymentResponse> r = rest.exchange(
                "/api/payments/rentals",
                HttpMethod.POST,
                jsonEntity(payReq),
                RentalPaymentResponse.class
        );
        // Assert
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        RentalPaymentResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.rentalId()).isEqualTo(rentalId);
        assertThat(body.originalAmount()).isEqualByComparingTo(payReq.amount());
        assertThat(body.originalCurrency()).isEqualTo("USD");
        assertThat(body.fxRateToEur()).isNotNull();
        assertThat(body.amountEur()).isNotNull();
        assertThat(body.status()).isEqualTo("PAID");
        assertThat(body.paidAt()).isNotNull();
    }

    @Test
    void should_list_payments_for_rental_and_return_200() {
        BikeResponse bike = createBike(new BikeCreateRequest(
                "Bike for list payments test",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("3.00")
        ));
        Long rentalId = rentBikeAndGetRentalId(bike.id(), customerId, 1);
        rest.exchange(
                "/api/payments/rentals",
                HttpMethod.POST,
                jsonEntity(new PayRentalRequest(rentalId, new BigDecimal("5.00"), "EUR", "pm_card_visa")),
                RentalPaymentResponse.class
        );
        // Keep your existing endpoint path from the pasted test
        ResponseEntity<List<RentalPaymentResponse>> r = rest.exchange(
                "/api/payments/rentals/" + rentalId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        List<RentalPaymentResponse> payments = r.getBody();
        assertThat(payments).isNotNull();
        assertThat(payments).isNotEmpty();
        assertThat(payments).allSatisfy(p -> {
            assertThat(p.id()).isNotNull();
            assertThat(p.rentalId()).isEqualTo(rentalId);
            assertThat(p.amountEur()).isNotNull();
            assertThat(p.status()).isEqualTo("PAID");
            assertThat(p.paidAt()).isNotNull();
        });
    }

    @Test
    void should_return_400_when_pay_request_is_invalid() {
        // If PayRentalRequest now includes paymentMethodId, keep it invalid too.
        PayRentalRequest invalid = new PayRentalRequest(null, null, "", "");
        ResponseEntity<String> r = rest.exchange(
                "/api/payments/rentals",
                HttpMethod.POST,
                jsonEntity(invalid),
                String.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        assertThat(r.getBody()).isNotBlank();
    }

    // -----------------------
    // Helpers
    // -----------------------
    private BikeResponse createBike(BikeCreateRequest req) {
        ResponseEntity<BikeResponse> r = rest.exchange(
                "/api/rental-offers",
                HttpMethod.POST,
                jsonEntity(req),
                BikeResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BikeResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        return body;
    }

    private Long rentBikeAndGetRentalId(Long bikeId, UUID customerId, int days) {
        ResponseEntity<RentBikeResultResponse> r = rest.exchange(
                "/api/rentals",
                HttpMethod.POST,
                jsonEntity(new RentBikeRequest(bikeId, customerId, days)),
                RentBikeResultResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        RentBikeResultResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.result()).isEqualTo(RentResult.RENTED);
        assertThat(body.rentalId()).isNotNull();
        return body.rentalId();
    }

    private <T> HttpEntity<T> jsonEntity(T body) {
        return new HttpEntity<>(body, authHeaders());
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
