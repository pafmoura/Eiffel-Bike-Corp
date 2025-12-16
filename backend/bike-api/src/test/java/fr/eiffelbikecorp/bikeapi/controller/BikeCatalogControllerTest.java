package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.dto.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.ProviderType;
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

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(MockitoExtension.class)
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BikeCatalogControllerTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository repository;

    private Long corpId = 1L;

    @BeforeEach
    void seedProvider() {
        EiffelBikeCorp corp = repository.findById(corpId).orElse(null);
        if (corp == null) {
            corp = new EiffelBikeCorp();
            corp.setName("Eiffel Bike Corp");
            corpId = repository.saveAndFlush(corp).getId();
        }
    }

    @Test
    void should_create_bike_and_return_201_with_response_body() {
        BikeCreateRequest req = new BikeCreateRequest(
                "City bike - good condition",
                ProviderType.CORP,
                corpId,
                new BigDecimal("2.50")
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<BikeCreateRequest> entity = new HttpEntity<>(req, headers);
        ResponseEntity<BikeResponse> r = rest.exchange(
                "/api/bikes",
                HttpMethod.POST,
                entity,
                BikeResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        BikeResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.description()).isEqualTo(req.description());
        assertThat(body.rentalDailyRateEur()).isEqualByComparingTo(req.rentalDailyRateEur());
        assertThat(body.offeredBy().type()).isEqualTo("CORP");
        assertThat(body.offeredBy().id()).isEqualTo(req.offeredById());
    }
}
