package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.Customer;
import fr.eiffelbikecorp.bikeapi.domain.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.dto.*;
import fr.eiffelbikecorp.bikeapi.payment.PaymentGateway;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static fr.eiffelbikecorp.bikeapi.Utils.randomEmail;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(MockitoExtension.class)
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentControllerTest {

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

    private UUID corpId = UUID.randomUUID();
    private UUID customerId = UUID.randomUUID();

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
            customerId = customerRepository.saveAndFlush(c).getId();
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
                randomPaymentMethodId()

        );
        // Act
        ResponseEntity<RentalPaymentResponse> r = rest.exchange(
                "/api/payments",
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
                "/api/payments",
                HttpMethod.POST,
                jsonEntity(new PayRentalRequest(rentalId, new BigDecimal("5.00"), "EUR", randomPaymentMethodId())),
                RentalPaymentResponse.class
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(customerId.toString());
        // Keep your existing endpoint path from the pasted test
        ResponseEntity<List<RentalPaymentResponse>> r = rest.exchange(
                "/api/payments/rentals/" + rentalId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
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
                "/api/payments",
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
                "/api/bikes",
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
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(customerId.toString()); // your token = customer UUID
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private static final List<String> PAYMENT_METHOD_IDS = List.of("pm_1SgN4CCaQMBvcQcbDYKaJorf",
            "pm_1SgN4BCaQMBvcQcb1y258mEv",
            "pm_1SgN4ACaQMBvcQcbw6uONmPV",
            "pm_1SgN49CaQMBvcQcbHZvWntvZ",
            "pm_1SgN48CaQMBvcQcb2De5Ixoj",
            "pm_1SgN47CaQMBvcQcbZ2ZBQyvz",
            "pm_1SgN45CaQMBvcQcbGIUe76Ij",
            "pm_1SgN44CaQMBvcQcb9zjlvYFG",
            "pm_1SgN43CaQMBvcQcbUgeYVpRO",
            "pm_1SgN42CaQMBvcQcbfvgy5U17",
            "pm_1SgN41CaQMBvcQcbgfUl4BIU",
            "pm_1SgN40CaQMBvcQcb0W5zF6d8",
            "pm_1SgN3zCaQMBvcQcbmk3gYs7W",
            "pm_1SgN3yCaQMBvcQcbF2FQe9EN",
            "pm_1SgN3xCaQMBvcQcbujVSBS7Z",
            "pm_1SgN3wCaQMBvcQcbkIXlFHy8",
            "pm_1SgN3wCaQMBvcQcb8Ga57f9o",
            "pm_1SgN3vCaQMBvcQcb7qRRh5gh",
            "pm_1SgN3uCaQMBvcQcbbhUDAbKV",
            "pm_1SgN3tCaQMBvcQcbrTMDdCBk",
            "pm_1SgN3sCaQMBvcQcbHtLu02SY",
            "pm_1SgN3rCaQMBvcQcbOfshqRZc",
            "pm_1SgN3qCaQMBvcQcbaYcvRCQK",
            "pm_1SgN3pCaQMBvcQcbNU1TuYGr",
            "pm_1SgN3oCaQMBvcQcbb6dXVt1t",
            "pm_1SgN3nCaQMBvcQcb3Uf2DSr7",
            "pm_1SgN3mCaQMBvcQcbNa2Ze2pB",
            "pm_1SgN3lCaQMBvcQcbe0qr0Pqv",
            "pm_1SgN3jCaQMBvcQcb1WiRWiAT",
            "pm_1SgN3iCaQMBvcQcbcAvBGD1i",
            "pm_1SgN3hCaQMBvcQcbHC0KJayw",
            "pm_1SgN3gCaQMBvcQcbFonRAtuO",
            "pm_1SgN3fCaQMBvcQcbq31SDvm9",
            "pm_1SgN3dCaQMBvcQcbh9xoUScy",
            "pm_1SgN3cCaQMBvcQcbwIy5SQCy",
            "pm_1SgN3cCaQMBvcQcbobtm5CQT",
            "pm_1SgN3bCaQMBvcQcbcYaNPSvU",
            "pm_1SgN3YCaQMBvcQcbjX4wvhgg",
            "pm_1SgN3XCaQMBvcQcbh25NoZQu",
            "pm_1SgN3WCaQMBvcQcbPXPQbu1r",
            "pm_1SgN3VCaQMBvcQcbXDtJwgGi",
            "pm_1SgN3UCaQMBvcQcbyhunm4X3",
            "pm_1SgN3UCaQMBvcQcbYBOMYs1S",
            "pm_1SgN3TCaQMBvcQcb4jvxEsWJ",
            "pm_1SgN3SCaQMBvcQcbay8bndUQ",
            "pm_1SgN3PCaQMBvcQcbe5vKhY7b",
            "pm_1SgN3OCaQMBvcQcbxpOUV3uW",
            "pm_1SgN3NCaQMBvcQcbVGf94P9A",
            "pm_1SgN3MCaQMBvcQcbnbjXkUtA",
            "pm_1SgN3LCaQMBvcQcbUCq9sRVz",
            "pm_1SgN3KCaQMBvcQcbpjDvLk1m",
            "pm_1SgN3JCaQMBvcQcbpbGVnNhC",
            "pm_1SgN3ICaQMBvcQcbpZo9jn8U",
            "pm_1SgN3HCaQMBvcQcbl5ZBaN7a",
            "pm_1SgN3GCaQMBvcQcbF1JBZydk",
            "pm_1SgN3FCaQMBvcQcbkzxXDL0w",
            "pm_1SgN3ECaQMBvcQcbYPf2fefK",
            "pm_1SgN3DCaQMBvcQcbspOkeu2n",
            "pm_1SgN3CCaQMBvcQcbsNUHXFLG",
            "pm_1SgN3BCaQMBvcQcbLNHWQv5H",
            "pm_1SgN3BCaQMBvcQcbdrMOY0St",
            "pm_1SgN3ACaQMBvcQcbMdIuQXzp",
            "pm_1SgN39CaQMBvcQcbeBiu0C5H",
            "pm_1SgN38CaQMBvcQcb83GwZwBw",
            "pm_1SgN37CaQMBvcQcbnJyEEDbA",
            "pm_1SgN36CaQMBvcQcbuFMD5Vj2",
            "pm_1SgN35CaQMBvcQcb44sRqAH5",
            "pm_1SgN34CaQMBvcQcbVnGwoEQZ",
            "pm_1SgN33CaQMBvcQcbkSW8sJZo",
            "pm_1SgN32CaQMBvcQcbH5KOQAWn",
            "pm_1SgN31CaQMBvcQcbKaVGZJw5",
            "pm_1SgN30CaQMBvcQcbsoLqW6ar"
    );

    public static String randomPaymentMethodId() {
        int i = ThreadLocalRandom.current().nextInt(PAYMENT_METHOD_IDS.size());
        return PAYMENT_METHOD_IDS.get(i);
    }
}
