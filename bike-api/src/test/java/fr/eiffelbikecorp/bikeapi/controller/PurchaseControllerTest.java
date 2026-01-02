package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.entity.Customer;
import fr.eiffelbikecorp.bikeapi.dto.response.BasketResponse;
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
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PurchaseControllerTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository corpRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    private TokenService tokenService;
    private String accessToken;

    @BeforeEach
    void setup() {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setEmail("purchase-test-" + UUID.randomUUID() + "@test.com");
        c.setFullName("Purchase Tester");
        c.setPassword("testpassword");
        c = customerRepository.saveAndFlush(c);
        accessToken = tokenService.generateToken(c);
    }

    @Test
    void should_return_409_when_checkout_with_empty_basket() {
        ResponseEntity<BasketResponse> openBasket = rest.exchange(
                "/api/basket",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                BasketResponse.class
        );
        assertThat(openBasket.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(openBasket.getBody()).isNotNull();
        assertThat(openBasket.getBody().items()).isEmpty();
        ResponseEntity<String> checkout = rest.exchange(
                "/api/purchases/checkout",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                String.class
        );
        assertThat(checkout.getStatusCode()).isEqualTo(HttpStatus.CONFLICT); // BusinessRuleException -> 409
        assertThat(checkout.getHeaders().getContentType()).isNotNull();
        assertThat(checkout.getHeaders().getContentType().toString()).contains("application/json");
        assertThat(checkout.getBody()).isNotBlank();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
