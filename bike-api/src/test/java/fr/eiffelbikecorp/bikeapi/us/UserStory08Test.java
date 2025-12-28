package fr.eiffelbikecorp.bikeapi.us;

import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.domain.enums.UserType;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.PayRentalRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.RentBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserLoginRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserRegisterRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.RentBikeResultResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.RentalPaymentResponse;
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
class UserStory08Test {
    // US_08: As a Customer, I want to pay the rental fee in any currency and have it converted to euros.

    private static final String API = "/api";

    @Autowired
    TestRestTemplate rest;

    private String studentToken;
    private UUID studentProviderId;
    private Long bikeId;

    private String customerToken;
    private UUID customerId;
    private Long rentalId;

    @BeforeEach
    void setup() {
        String password = "secret123";

        // 1) Student offers one bike
        String studentEmail = "student+" + UUID.randomUUID() + "@example.com";

        ResponseEntity<UserResponse> studentRegisterResp = rest.exchange(
                API + "/users/register",
                HttpMethod.POST,
                new HttpEntity<>(new UserRegisterRequest(
                        UserType.STUDENT,
                        "Student Provider",
                        studentEmail,
                        password
                ), jsonHeaders()),
                UserResponse.class
        );
        assertThat(studentRegisterResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(studentRegisterResp.getBody()).isNotNull();
        this.studentProviderId = studentRegisterResp.getBody().providerId();
        assertThat(studentProviderId).isNotNull();

        ResponseEntity<UserLoginResponse> studentLoginResp = rest.exchange(
                API + "/users/login",
                HttpMethod.POST,
                new HttpEntity<>(new UserLoginRequest(studentEmail, password), jsonHeaders()),
                UserLoginResponse.class
        );
        assertThat(studentLoginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(studentLoginResp.getBody()).isNotNull();
        this.studentToken = studentLoginResp.getBody().accessToken();
        assertThat(studentToken).isNotBlank();

        ResponseEntity<BikeResponse> bikeCreateResp = rest.exchange(
                API + "/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(new BikeCreateRequest(
                        "Bike for paid rental",
                        ProviderType.STUDENT,
                        studentProviderId,
                        new BigDecimal("2.50")
                ), authJsonHeaders(studentToken)),
                BikeResponse.class
        );
        assertThat(bikeCreateResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bikeCreateResp.getBody()).isNotNull();
        this.bikeId = bikeCreateResp.getBody().id();
        assertThat(bikeId).isNotNull();

        // 2) Customer registers + logs in
        String customerEmail = "customer+" + UUID.randomUUID() + "@example.com";

        ResponseEntity<UserResponse> customerRegisterResp = rest.exchange(
                API + "/users/register",
                HttpMethod.POST,
                new HttpEntity<>(new UserRegisterRequest(
                        UserType.CUSTOMER,
                        "Customer One",
                        customerEmail,
                        password
                ), jsonHeaders()),
                UserResponse.class
        );
        assertThat(customerRegisterResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(customerRegisterResp.getBody()).isNotNull();
        this.customerId = customerRegisterResp.getBody().customerId();
        assertThat(customerId).isNotNull();

        ResponseEntity<UserLoginResponse> customerLoginResp = rest.exchange(
                API + "/users/login",
                HttpMethod.POST,
                new HttpEntity<>(new UserLoginRequest(customerEmail, password), jsonHeaders()),
                UserLoginResponse.class
        );
        assertThat(customerLoginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(customerLoginResp.getBody()).isNotNull();
        this.customerToken = customerLoginResp.getBody().accessToken();
        assertThat(customerToken).isNotBlank();

        // 3) Customer rents the bike
        ResponseEntity<RentBikeResultResponse> rentResp = rest.exchange(
                API + "/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(bikeId, customerId, 2), authJsonHeaders(customerToken)),
                RentBikeResultResponse.class
        );

        assertThat(rentResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(rentResp.getBody()).isNotNull();
        assertThat(rentResp.getBody().result()).isEqualTo(RentResult.RENTED);
        assertThat(rentResp.getBody().rentalId()).isNotNull();
        this.rentalId = rentResp.getBody().rentalId();
    }

    @Test
    void should_pay_rental_in_foreign_currency_and_receive_eur_converted_amount() {
        // Given: Pay rental with USD
        PayRentalRequest payReq = new PayRentalRequest(
                rentalId,
                new BigDecimal("10.00"),
                "USD",
                "pm_card_visa"
        );

        // When
        ResponseEntity<RentalPaymentResponse> payResp = rest.exchange(
                API + "/payments/rentals",
                HttpMethod.POST,
                new HttpEntity<>(payReq, authJsonHeaders(customerToken)),
                RentalPaymentResponse.class
        );

        // Then
        assertThat(payResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(payResp.getBody()).isNotNull();

        RentalPaymentResponse payment = payResp.getBody();

        // Verify original payment request data echoed back
        assertThat(payment.rentalId()).isEqualTo(rentalId);
        assertThat(payment.originalAmount()).isEqualByComparingTo("10.00");
        assertThat(payment.originalCurrency()).isEqualTo("USD");

        // Verify conversion fields exist and are coherent
        assertThat(payment.fxRateToEur()).as("fxRateToEur must be present").isNotNull();
        assertThat(payment.fxRateToEur()).isGreaterThan(BigDecimal.ZERO);

        assertThat(payment.amountEur()).as("amountEur must be present").isNotNull();
        assertThat(payment.amountEur()).isGreaterThan(BigDecimal.ZERO);

        // Coherence check: amountEur ~= originalAmount * fxRateToEur
        BigDecimal expectedEur = payment.originalAmount().multiply(payment.fxRateToEur());
        assertThat(payment.amountEur())
                .as("amountEur should match originalAmount * fxRateToEur (within rounding)")
                .isCloseTo(expectedEur, org.assertj.core.data.Offset.offset(new BigDecimal("0.05")));

        // Status/paidAt should be set on successful payment flows
        assertThat(payment.status()).isNotNull();
        assertThat(payment.paidAt()).isNotNull();

        log.info("US_08 OK - rentalId={}, paid {} {} => {} EUR @ rate {}",
                rentalId,
                payment.originalAmount(), payment.originalCurrency(),
                payment.amountEur(), payment.fxRateToEur());
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
