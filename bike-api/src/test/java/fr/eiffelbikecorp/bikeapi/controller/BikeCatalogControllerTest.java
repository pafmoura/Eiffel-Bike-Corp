package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.Customer;
import fr.eiffelbikecorp.bikeapi.domain.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.dto.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.BikeUpdateRequest;
import fr.eiffelbikecorp.bikeapi.dto.ProviderType;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
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

import static fr.eiffelbikecorp.bikeapi.Utils.randomEmail;
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

    private UUID corpId = UUID.randomUUID();
    private UUID autenticatedCustumerId = UUID.randomUUID();
    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void seedProvider() {
        EiffelBikeCorp corp = repository.findById(corpId).orElse(null);
        if (corp == null) {
            corp = new EiffelBikeCorp();
            corp.setId(UUID.randomUUID());
            corpId = repository.saveAndFlush(corp).getId();
        }
        Customer customer = customerRepository.findById(autenticatedCustumerId).orElse(null);
        if (customer == null) {
            Customer c = new Customer();
            c.setEmail(randomEmail());
            c.setFullName("Bike Catalog Tester");
            autenticatedCustumerId = customerRepository.saveAndFlush(c).getId();
        }

    }

    private BikeResponse createBike(BikeCreateRequest req) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(autenticatedCustumerId.toString());
        ResponseEntity<BikeResponse> r = rest.exchange(
                "/api/bikes",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                BikeResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BikeResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        return body;
    }

    @Test
    void should_create_bike_and_return_201_with_response_body() {
        BikeCreateRequest req = new BikeCreateRequest(
                "City bike - good condition",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.50")
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(autenticatedCustumerId.toString());
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

    @Test
    void should_update_bike_and_return_200_with_updated_body() {
        BikeCreateRequest create = new BikeCreateRequest(
                "Bike to update",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("3.00")
        );
        BikeResponse createdBody = createBike(create);
        Long bikeId = createdBody.id();
        assertThat(bikeId).isNotNull();
        BikeUpdateRequest update = new BikeUpdateRequest(
                "Updated description",
                "MAINTENANCE",
                new BigDecimal("4.50")
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(autenticatedCustumerId.toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<BikeResponse> updated = rest.exchange(
                "/api/bikes/" + bikeId,
                HttpMethod.PUT,
                new HttpEntity<>(update, headers),
                BikeResponse.class
        );
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getHeaders().getContentType()).isNotNull();
        assertThat(updated.getHeaders().getContentType().toString()).contains("application/json");
        BikeResponse body = updated.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(bikeId);
        assertThat(body.description()).isEqualTo(update.description());
        assertThat(body.status()).isEqualTo("MAINTENANCE");
        assertThat(body.rentalDailyRateEur()).isEqualByComparingTo(update.rentalDailyRateEur());
    }

    @Test
    void should_filter_bikes_by_status_q_and_offeredById() {
        // Create bikes (one should match)
        BikeCreateRequest match = new BikeCreateRequest(
                "Trek city bike",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.00")
        );
        BikeCreateRequest other = new BikeCreateRequest(
                "Road bike",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.00")
        );
        createBike(match);
        BikeResponse otherCreated = createBike(other);
        // Make "other" not match by changing its status
        BikeUpdateRequest makeMaintenance = new BikeUpdateRequest(
                null,
                "MAINTENANCE",
                null
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(autenticatedCustumerId.toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        rest.exchange(
                "/api/bikes/" + otherCreated.id(),
                HttpMethod.PUT,
                new HttpEntity<>(makeMaintenance, headers),
                BikeResponse.class
        );
        String url = "/api/bikes?status=AVAILABLE&q=trek&offeredById=" + corpId;
        ResponseEntity<List<BikeResponse>> filteredResp = rest.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(filteredResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(filteredResp.getHeaders().getContentType()).isNotNull();
        assertThat(filteredResp.getHeaders().getContentType().toString()).contains("application/json");
        List<BikeResponse> filtered = filteredResp.getBody();
        assertThat(filtered).isNotNull();
        assertThat(filtered).isNotEmpty();
        // All returned items must satisfy the filter conditions
        assertThat(filtered).allSatisfy(b -> {
            assertThat(b.status()).isEqualTo("AVAILABLE");
            assertThat(b.description().toLowerCase()).contains("trek");
            assertThat(b.offeredBy().id()).isEqualTo(corpId);
        });
        // And at least one is exactly our matching bike
        assertThat(filtered).anySatisfy(b -> assertThat(b.description()).isEqualTo(match.description()));
    }

    @Test
    void should_return_400_when_create_request_is_invalid() {
        BikeCreateRequest invalid = new BikeCreateRequest(
                "",                         // @NotBlank
                null,                       // @NotNull
                null,                       // @NotNull
                null                        // @NotNull
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(autenticatedCustumerId.toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> r = rest.exchange(
                "/api/bikes",
                HttpMethod.POST,
                new HttpEntity<>(invalid, headers),
                String.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        assertThat(r.getBody()).isNotBlank();
    }
}
