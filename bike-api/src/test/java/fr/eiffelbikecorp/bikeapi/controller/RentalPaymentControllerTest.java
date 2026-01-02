package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.entity.Customer;
import fr.eiffelbikecorp.bikeapi.domain.entity.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.PayRentalRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.RentBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.RentBikeResultResponse;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
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
import org.springframework.http.*;
import org.springframework.orm.jpa.EntityManagerFactoryAccessor;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static fr.eiffelbikecorp.bikeapi.Utils.randomEmail;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

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
