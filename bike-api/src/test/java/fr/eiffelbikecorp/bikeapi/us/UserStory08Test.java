package fr.eiffelbikecorp.bikeapi.us;

import fr.eiffelbikecorp.bikeapi.domain.enums.BikeStatus;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.UserType;
import fr.eiffelbikecorp.bikeapi.dto.request.*;
import fr.eiffelbikecorp.bikeapi.dto.response.*;
import lombok.extern.slf4j.Slf4j;
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
class UserStory08Test {
    // US_08: As Student or Employee,
    // I want to pay the rental fee in any currency and have it converted to euros.
    private static final String API = "/api";
    @Autowired
    TestRestTemplate rest;

    private UUID providerId;
    private UUID customerId;

    private Long bikeId;
    private Long rentalId;
    private BigDecimal price = new BigDecimal("10.00");

    private String accessToken;

    void createProviderAndOfferBikeForRent(UserType userType) {
        String studentEmail = UUID.randomUUID() + "@example.com";
        String password = "secret123";
        ResponseEntity<UserResponse> studentRegisterResp = rest.exchange(
                API + "/users/register",
                HttpMethod.POST,
                new HttpEntity<>(new UserRegisterRequest(
                        userType,
                        "Student Provider",
                        studentEmail,
                        password
                ), jsonHeaders()),
                UserResponse.class
        );
        assertThat(studentRegisterResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(studentRegisterResp.getBody()).isNotNull();
        assertThat(studentRegisterResp.getBody().providerId()).isNotNull();
        this.providerId = studentRegisterResp.getBody().providerId();
        ResponseEntity<UserLoginResponse> studentLoginResp = rest.exchange(
                API + "/users/login",
                HttpMethod.POST,
                new HttpEntity<>(new UserLoginRequest(studentEmail, password), jsonHeaders()),
                UserLoginResponse.class
        );
        assertThat(studentLoginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(studentLoginResp.getBody()).isNotNull();
        accessToken = studentLoginResp.getBody().accessToken();
        assertThat(accessToken).isNotBlank();
        ResponseEntity<BikeResponse> bikeCreateResp = rest.exchange(
                API + "/rental-offers",
                HttpMethod.POST,
                new HttpEntity<>(new BikeCreateRequest(
                        "Commute bike - available",
                        ProviderType.STUDENT,
                        providerId,
                        price
                ), authJsonHeaders(accessToken)),
                BikeResponse.class
        );
        assertThat(bikeCreateResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bikeCreateResp.getBody()).isNotNull();
        this.bikeId = bikeCreateResp.getBody().id();
        assertThat(bikeId).isNotNull();
    }

    void createCustomer_1_ForRent(UserType userType) {
        String customerEmail = UUID.randomUUID() + "@example.com";
        String password = "secret123";
        ResponseEntity<UserResponse> customerRegisterResp = rest.exchange(
                API + "/users/register",
                HttpMethod.POST,
                new HttpEntity<>(new UserRegisterRequest(
                        userType,
                        "Renter 1 " + userType.name(),
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
        this.accessToken = customerLoginResp.getBody().accessToken();
        assertThat(accessToken).isNotBlank();
    }

    void rent_a_bike() {
        RentBikeRequest rentReq = new RentBikeRequest(bikeId, customerId, 3);
        ResponseEntity<RentBikeResultResponse> rentResp = rest.exchange(
                API + "/rentals",
                HttpMethod.POST,
                new HttpEntity<>(rentReq, authJsonHeaders(accessToken)),
                RentBikeResultResponse.class
        );
        assertThat(rentResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(rentResp.getBody()).isNotNull();
        assertThat(rentResp.getBody().result()).isNotNull();
        assertThat(BikeStatus.valueOf(rentResp.getBody().result().toString())).isEqualTo(BikeStatus.RENTED);
        assertThat(rentResp.getBody().rentalId()).as("rentalId should be present when RENTED").isNotNull();
        assertThat(rentResp.getBody().waitingListEntryId()).as("waitingListEntryId should be null when RENTED").isNull();
        ParameterizedTypeReference<List<BikeResponse>> listType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<List<BikeResponse>> listResp = rest.exchange(
                API + "/bikes?offeredById=" + providerId,
                HttpMethod.GET,
                new HttpEntity<>(authJsonHeaders(accessToken)),
                listType
        );
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).isNotNull();
        BikeResponse rentedBike = listResp.getBody().stream()
                .filter(b -> b.id().equals(bikeId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected to find bike " + bikeId + " in provider bikes"));
        assertThat(rentedBike.status()).isEqualTo("RENTED");
        rentalId = rentResp.getBody().rentalId();
    }

    @Test
    void should_pay_rental_in_foreign_currency_and_receive_eur_converted_amount() {
        createProviderAndOfferBikeForRent(UserType.STUDENT);
        createCustomer_1_ForRent(UserType.STUDENT);
        rent_a_bike();
        PayRentalRequest payReq = new PayRentalRequest(
                rentalId,
                price,
                "USD",
                "pm_card_visa"
        );
        ResponseEntity<RentalPaymentResponse> payResp = rest.exchange(
                API + "/payments/rentals",
                HttpMethod.POST,
                new HttpEntity<>(payReq, authJsonHeaders(accessToken)),
                RentalPaymentResponse.class
        );
        assertThat(payResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(payResp.getBody()).isNotNull();
        RentalPaymentResponse payment = payResp.getBody();
        assertThat(payment.rentalId()).isEqualTo(rentalId);
        assertThat(payment.originalAmount()).isEqualByComparingTo("10.00");
        assertThat(payment.originalCurrency()).isEqualTo("USD");
        assertThat(payment.fxRateToEur()).as("fxRateToEur must be present").isNotNull();
        assertThat(payment.fxRateToEur()).isGreaterThan(BigDecimal.ZERO);
        assertThat(payment.amountEur()).as("amountEur must be present").isNotNull();
        assertThat(payment.amountEur()).isGreaterThan(BigDecimal.ZERO);
        // Coherence check: amountEur ~= originalAmount * fxRateToEur
        BigDecimal expectedEur = payment.originalAmount().multiply(payment.fxRateToEur());
        assertThat(payment.amountEur())
                .as("amountEur should match originalAmount * fxRateToEur (within rounding)")
                .isCloseTo(expectedEur, org.assertj.core.data.Offset.offset(new BigDecimal("0.05")));
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
