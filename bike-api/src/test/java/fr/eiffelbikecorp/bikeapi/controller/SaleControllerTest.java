package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.Customer;
import fr.eiffelbikecorp.bikeapi.domain.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.dto.*;
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
class SaleControllerTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository corpRepository;

    @Autowired
    CustomerRepository customerRepository;

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
            c.setFullName("Sale Tester");
            customerId = customerRepository.saveAndFlush(c).getId();
        }
    }

    @Test
    void should_create_sale_offer_and_return_201_with_response_body() {
        BikeResponse bike = createBike(new BikeCreateRequest(
                "Bike for sale offer test",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.50")
        ));
        CreateSaleOfferRequest req = new CreateSaleOfferRequest(
                bike.id(),
                corpId,
                new BigDecimal("250.00")
        );
        ResponseEntity<SaleOfferResponse> r = rest.exchange(
                "/api/sales/offers",
                HttpMethod.POST,
                jsonEntity(req),
                SaleOfferResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        SaleOfferResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.bikeId()).isEqualTo(bike.id());
        assertThat(body.sellerCorpId()).isEqualTo(corpId);
        assertThat(body.askingPriceEur()).isEqualByComparingTo(req.askingPriceEur());
        assertThat(body.status()).isEqualTo("LISTED");
        assertThat(body.listedAt()).isNotNull();
        assertThat(body.soldAt()).isNull();
        assertThat(body.buyerCustomerId()).isNull();
    }

    @Test
    void should_add_sale_note_and_return_201_with_response_body() {
        BikeResponse bike = createBike(new BikeCreateRequest(
                "Bike for sale note test",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.00")
        ));
        SaleOfferResponse offer = createSaleOffer(bike.id(), corpId, new BigDecimal("180.00"));
        CreateSaleNoteRequest req = new CreateSaleNoteRequest(
                offer.id(),
                "Excellent condition",
                "Brakes and tires recently replaced. No rust.",
                "Alice"
        );
        ResponseEntity<SaleNoteResponse> r = rest.exchange(
                "/api/sales/notes",
                HttpMethod.POST,
                jsonEntity(req),
                SaleNoteResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        SaleNoteResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.saleOfferId()).isEqualTo(offer.id());
        assertThat(body.title()).isEqualTo(req.title());
        assertThat(body.content()).isEqualTo(req.content());
        assertThat(body.createdAt()).isNotNull();
        assertThat(body.createdBy()).isEqualTo(req.createdBy());
    }

    @Test
    void should_search_sale_offers_and_return_200() {
        BikeResponse bike1 = createBike(new BikeCreateRequest(
                "Mountain bike Trek X",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.00")
        ));
        BikeResponse bike2 = createBike(new BikeCreateRequest(
                "City bike classic",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.00")
        ));
        createSaleOffer(bike1.id(), corpId, new BigDecimal("300.00"));
        createSaleOffer(bike2.id(), corpId, new BigDecimal("150.00"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(customerId.toString());
        ResponseEntity<List<SaleOfferResponse>> r = rest.exchange(
                "/api/sales/offers?q=trek",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        List<SaleOfferResponse> results = r.getBody();
        assertThat(results).isNotNull();
        assertThat(results).isNotEmpty();
        // At least one should match the "trek" bike
        assertThat(results).anySatisfy(o -> assertThat(o.bikeId()).isEqualTo(bike1.id()));
    }

    @Test
    void should_get_sale_offer_details_by_bike_and_return_200_with_notes() {
        BikeResponse bike = createBike(new BikeCreateRequest(
                "Bike for details test",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.00")
        ));
        SaleOfferResponse offer = createSaleOffer(bike.id(), corpId, new BigDecimal("199.00"));
        // add two notes
        rest.exchange(
                "/api/sales/notes",
                HttpMethod.POST,
                jsonEntity(new CreateSaleNoteRequest(offer.id(), "Note 1", "Content 1", "Bob")),
                SaleNoteResponse.class
        );
        rest.exchange(
                "/api/sales/notes",
                HttpMethod.POST,
                jsonEntity(new CreateSaleNoteRequest(offer.id(), "Note 2", "Content 2", "Bob")),
                SaleNoteResponse.class
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(customerId.toString());
        ResponseEntity<SaleOfferDetailsResponse> r = rest.exchange(
                "/api/sales/bikes/" + bike.id(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                SaleOfferDetailsResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        SaleOfferDetailsResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.offer()).isNotNull();
        assertThat(body.offer().id()).isEqualTo(offer.id());
        assertThat(body.offer().bikeId()).isEqualTo(bike.id());
        assertThat(body.notes()).isNotNull();
        assertThat(body.notes().size()).isGreaterThanOrEqualTo(2);
        assertThat(body.notes()).anySatisfy(n -> assertThat(n.title()).isEqualTo("Note 1"));
        assertThat(body.notes()).anySatisfy(n -> assertThat(n.title()).isEqualTo("Note 2"));
    }

    @Test
    void should_return_400_when_create_sale_offer_request_is_invalid() {
        CreateSaleOfferRequest invalid = new CreateSaleOfferRequest(
                null,
                null,
                null
        );
        ResponseEntity<String> r = rest.exchange(
                "/api/sales/offers",
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

    private SaleOfferResponse createSaleOffer(Long bikeId, UUID sellerCorpId, BigDecimal askingPriceEur) {
        ResponseEntity<SaleOfferResponse> r = rest.exchange(
                "/api/sales/offers",
                HttpMethod.POST,
                jsonEntity(new CreateSaleOfferRequest(bikeId, sellerCorpId, askingPriceEur)),
                SaleOfferResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        SaleOfferResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        return body;
    }

    private <T> HttpEntity<T> jsonEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(customerId.toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
