package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.Customer;
import fr.eiffelbikecorp.bikeapi.domain.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.domain.Student;
import fr.eiffelbikecorp.bikeapi.dto.*;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
import fr.eiffelbikecorp.bikeapi.persistence.StudentRepository;
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
class SaleOfferControllerTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository corpRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    StudentRepository studentRepository;

    private UUID corpId;
    private UUID customerId1;

    @BeforeEach
    void setup() {
        // Seed corp
        EiffelBikeCorp corp = new EiffelBikeCorp();
        corpId = corpRepository.saveAndFlush(corp).getId();

        // Seed customer (token = customer UUID)
        Customer c1 = new Customer();
        c1.setId(UUID.randomUUID());
        c1.setEmail("sale-offer-test-" + UUID.randomUUID() + "@test.com");
        c1.setFullName("Buyer One");
        customerId1 = customerRepository.saveAndFlush(c1).getId();
    }

    @Test
    void should_create_sale_offer_and_return_201_when_bike_is_corp_and_rented_once() {
        BikeResponse bike = createBike(new BikeCreateRequest(
                "Corp bike for sale listing",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.50")
        ));

        // Eligibility rule: must be rented at least once
        rentBikeOnceAndReturn(bike.id(), customerId1);

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
        assertThat(body.askingPriceEur()).isEqualByComparingTo(req.askingPriceEur());
        assertThat(body.status()).isEqualTo("LISTED");
        assertThat(body.listedAt()).isNotNull();
    }

    @Test
    void should_add_sale_note_and_return_201() {
        BikeResponse bike = createBike(new BikeCreateRequest(
                "Corp bike for notes",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.00")
        ));
        rentBikeOnceAndReturn(bike.id(), customerId1);

        SaleOfferResponse offer = createSaleOffer(bike.id(), corpId, new BigDecimal("180.00"));

        CreateSaleNoteRequest req = new CreateSaleNoteRequest(
                offer.id(),
                "Excellent condition",
                "Brakes and tires recently replaced. No rust.",
                "Alice"
        );

        ResponseEntity<SaleNoteResponse> r = rest.exchange(
                "/api/sales/offers/notes",
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
        assertThat(body.createdBy()).isEqualTo(req.createdBy());
        assertThat(body.createdAt()).isNotNull();
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

        rentBikeOnceAndReturn(bike1.id(), customerId1);
        rentBikeOnceAndReturn(bike2.id(), customerId1);

        createSaleOffer(bike1.id(), corpId, new BigDecimal("300.00"));
        createSaleOffer(bike2.id(), corpId, new BigDecimal("150.00"));

        ResponseEntity<List<SaleOfferResponse>> r = rest.exchange(
                "/api/sales/offers?q=trek",
                HttpMethod.GET,
                null, // public endpoint
                new ParameterizedTypeReference<>() {}
        );

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");

        List<SaleOfferResponse> results = r.getBody();
        assertThat(results).isNotNull();
        assertThat(results).isNotEmpty();
        assertThat(results).anySatisfy(o -> assertThat(o.bikeId()).isEqualTo(bike1.id()));
    }

    @Test
    void should_get_sale_offer_details_by_offer_id_and_return_200_with_notes() {
        BikeResponse bike = createBike(new BikeCreateRequest(
                "Bike for details by offerId",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.00")
        ));
        rentBikeOnceAndReturn(bike.id(), customerId1);

        SaleOfferResponse offer = createSaleOffer(bike.id(), corpId, new BigDecimal("199.00"));

        // add notes
        rest.exchange(
                "/api/sales/offers/notes",
                HttpMethod.POST,
                jsonEntity(new CreateSaleNoteRequest(offer.id(), "Note 1", "Content 1", "Bob")),
                SaleNoteResponse.class
        );
        rest.exchange(
                "/api/sales/offers/notes",
                HttpMethod.POST,
                jsonEntity(new CreateSaleNoteRequest(offer.id(), "Note 2", "Content 2", "Bob")),
                SaleNoteResponse.class
        );

        ResponseEntity<SaleOfferDetailsResponse> r = rest.exchange(
                "/api/sales/offers/" + offer.id(),
                HttpMethod.GET,
                null, // public endpoint
                SaleOfferDetailsResponse.class
        );

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        SaleOfferDetailsResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.offer()).isNotNull();
        assertThat(body.offer().id()).isEqualTo(offer.id());
        assertThat(body.offer().bikeId()).isEqualTo(bike.id());

        assertThat(body.notes()).isNotNull();
        assertThat(body.notes()).isNotEmpty();
        assertThat(body.notes()).anySatisfy(n -> assertThat(n.title()).isEqualTo("Note 1"));
        assertThat(body.notes()).anySatisfy(n -> assertThat(n.title()).isEqualTo("Note 2"));
    }

    @Test
    void should_get_sale_offer_details_by_bike_id_and_return_200() {
        BikeResponse bike = createBike(new BikeCreateRequest(
                "Bike for details by bikeId",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.00")
        ));
        rentBikeOnceAndReturn(bike.id(), customerId1);

        SaleOfferResponse offer = createSaleOffer(bike.id(), corpId, new BigDecimal("210.00"));

        ResponseEntity<SaleOfferDetailsResponse> r = rest.exchange(
                "/api/sales/offers/by-bike/" + bike.id(),
                HttpMethod.GET,
                null, // public endpoint
                SaleOfferDetailsResponse.class
        );

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        SaleOfferDetailsResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.offer()).isNotNull();
        assertThat(body.offer().id()).isEqualTo(offer.id());
        assertThat(body.offer().bikeId()).isEqualTo(bike.id());
    }

    @Test
    void should_return_409_when_bike_was_never_rented() {
        BikeResponse bike = createBike(new BikeCreateRequest(
                "Corp bike never rented",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.50")
        ));

        CreateSaleOfferRequest req = new CreateSaleOfferRequest(
                bike.id(),
                corpId,
                new BigDecimal("250.00")
        );

        ResponseEntity<String> r = rest.exchange(
                "/api/sales/offers",
                HttpMethod.POST,
                jsonEntity(req),
                String.class
        );

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody()).isNotBlank();
    }

    @Test
    void should_return_409_when_bike_is_not_corp_owned() {
        Student student = new Student();
        UUID studentId = studentRepository.saveAndFlush(student).getId();

        BikeResponse bike = createBike(new BikeCreateRequest(
                "Student bike - cannot be sold by corp",
                ProviderType.STUDENT,
                studentId,
                new BigDecimal("2.00")
        ));

        // Even if rented, should still be rejected by "corp-only" rule
        rentBikeOnceAndReturn(bike.id(), customerId1);

        CreateSaleOfferRequest req = new CreateSaleOfferRequest(
                bike.id(),
                corpId,
                new BigDecimal("199.00")
        );

        ResponseEntity<String> r = rest.exchange(
                "/api/sales/offers",
                HttpMethod.POST,
                jsonEntity(req),
                String.class
        );

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody()).isNotBlank();
    }

    @Test
    void should_return_400_when_create_sale_offer_request_is_invalid() {
        CreateSaleOfferRequest invalid = new CreateSaleOfferRequest(null, null, null);

        ResponseEntity<String> r = rest.exchange(
                "/api/sales/offers",
                HttpMethod.POST,
                jsonEntity(invalid),
                String.class
        );

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody()).isNotBlank();
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

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

    private void rentBikeOnceAndReturn(Long bikeId, UUID customerId) {
        // Rent
        ResponseEntity<RentBikeResultResponse> rented = rest.exchange(
                "/api/rentals",
                HttpMethod.POST,
                jsonEntity(new RentBikeRequest(bikeId, customerId, 1)),
                RentBikeResultResponse.class
        );
        assertThat(rented.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        RentBikeResultResponse rentedBody = rented.getBody();
        assertThat(rentedBody).isNotNull();
        assertThat(rentedBody.result()).isEqualTo(RentResult.RENTED);
        assertThat(rentedBody.rentalId()).isNotNull();

        // Return (so bike becomes available again for other tests)
        ResponseEntity<ReturnBikeResponse> returned = rest.exchange(
                "/api/rentals/" + rentedBody.rentalId() + "/return",
                HttpMethod.POST,
                jsonEntity(new ReturnBikeRequest(customerId, "return for sale eligibility", "Good")),
                ReturnBikeResponse.class
        );
        assertThat(returned.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(customerId1.toString()); // token = customer UUID
        return headers;
    }

    private <T> HttpEntity<T> jsonEntity(T body) {
        return new HttpEntity<>(body, authHeaders());
    }
}
