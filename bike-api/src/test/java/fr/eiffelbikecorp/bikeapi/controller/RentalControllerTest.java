package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.entity.Customer;
import fr.eiffelbikecorp.bikeapi.domain.entity.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.RentBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.ReturnBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.NotificationResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.RentBikeResultResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.ReturnBikeResponse;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
import fr.eiffelbikecorp.bikeapi.persistence.StudentRepository;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static fr.eiffelbikecorp.bikeapi.Utils.randomEmail;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(MockitoExtension.class)
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RentalControllerTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EiffelBikeCorpRepository corpRepository;

    @Autowired
    CustomerRepository customerRepository;

    private UUID corpId = UUID.randomUUID();
    private UUID customerId1 = UUID.randomUUID();
    private UUID customerId2 = UUID.randomUUID();

    @Autowired
    private TokenService tokenService;


    @BeforeEach
    void setup() {
        EiffelBikeCorp corp = corpRepository.findById(corpId).orElse(null);
        Customer customer1 = customerRepository.findById(customerId1).orElse(null);
        Customer customer2 = customerRepository.findById(customerId2).orElse(null);
        if (corp == null) {
            corp = new EiffelBikeCorp();
            corpId = corpRepository.saveAndFlush(corp).getId();
        }
        if (customer1 == null) {
            Customer c1 = new Customer();
            c1.setEmail(randomEmail());
            c1.setFullName("John Doe");
            c1.setPassword("testpassword");
            c1 = customerRepository.saveAndFlush(c1);
            customerId1 = c1.getId();
        }
        if (customer2 == null) {
            Customer c2 = new Customer();
            c2.setEmail(randomEmail());
            c2.setFullName("John Doe2");
            c2.setPassword("testpassword");
            customerId2 = customerRepository.saveAndFlush(c2).getId();
        }
    }

    @Test
    void should_rent_bike_and_return_201() {
        BikeResponse bike = createBike(new BikeCreateRequest(
                "Bike for rent test",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.50")
        ));
        RentBikeRequest rentReq = new RentBikeRequest(bike.id(), customerId1, 3);
        ResponseEntity<RentBikeResultResponse> r = rest.exchange(
                "/api/rentals",
                HttpMethod.POST,
                new HttpEntity<>(rentReq, authHeaders(tokenService.generateToken(
                        Objects.requireNonNull(customerRepository.findById(customerId1).orElse(null))
                ))),
                RentBikeResultResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        RentBikeResultResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.result()).isEqualTo(RentResult.RENTED);
        assertThat(body.rentalId()).isNotNull();
        assertThat(body.waitingListEntryId()).isNull();
    }

    @Test
    void should_waitlist_when_bike_not_available_and_return_202() {
        BikeResponse bike = createBike(new BikeCreateRequest(
                "Bike for waitlist test",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.00")
        ));
        // First customer rents -> 201
        ResponseEntity<RentBikeResultResponse> firstRent = rest.exchange(
                "/api/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(bike.id(), customerId1, 2), authHeaders(
                        tokenService.generateToken(
                                Objects.requireNonNull(customerRepository.findById(customerId1).orElse(null))
                        )
                )),
                RentBikeResultResponse.class
        );
        assertThat(firstRent.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // Second customer tries -> should be WAITLISTED -> 202
        ResponseEntity<RentBikeResultResponse> secondRent = rest.exchange(
                "/api/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(bike.id(), customerId2, 2), authHeaders(
                        tokenService.generateToken(
                                Objects.requireNonNull(customerRepository.findById(customerId2).orElse(null))
                        )
                )),
                RentBikeResultResponse.class
        );
        assertThat(secondRent.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        RentBikeResultResponse body = secondRent.getBody();
        assertThat(body).isNotNull();
        assertThat(body.result()).isEqualTo(RentResult.WAITLISTED);
        assertThat(body.rentalId()).isNull();
        assertThat(body.waitingListEntryId()).isNotNull();
    }

    @Test
    void should_return_bike_close_rental_create_next_rental_and_notification() {
        BikeResponse bike = createBike(new BikeCreateRequest(
                "Bike for return+notify test",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("3.00")
        ));
        // First rents
        RentBikeResultResponse firstRent = rest.exchange(
                "/api/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(bike.id(), customerId1, 2), authHeaders(
                        tokenService.generateToken(
                                Objects.requireNonNull(customerRepository.findById(customerId1).orElse(null))
                        )
                )),
                RentBikeResultResponse.class
        ).getBody();
        assertThat(firstRent).isNotNull();
        assertThat(firstRent.result()).isEqualTo(RentResult.RENTED);
        Long rentalId = firstRent.rentalId();
        assertThat(rentalId).isNotNull();
        // Second joins waitlist
        ResponseEntity<RentBikeResultResponse> secondRent = rest.exchange(
                "/api/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(bike.id(), customerId2, 1), authHeaders(
                        tokenService.generateToken(
                                Objects.requireNonNull(customerRepository.findById(customerId2).orElse(null))
                        )
                )),
                RentBikeResultResponse.class
        );
        assertThat(secondRent.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        // Return bike (adds note + triggers FIFO assignment + notification)
        ReturnBikeRequest returnReq = new ReturnBikeRequest(
                customerId1,
                "Returned in good condition",
                "Good"
        );
        ResponseEntity<ReturnBikeResponse> returned = rest.exchange(
                "/api/rentals/" + rentalId + "/return",
                HttpMethod.POST,
                new HttpEntity<>(returnReq, authHeaders(
                        tokenService.generateToken(
                                Objects.requireNonNull(customerRepository.findById(customerId1).orElse(null))
                        )
                )),
                ReturnBikeResponse.class
        );
        assertThat(returned.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(returned.getHeaders().getContentType()).isNotNull();
        assertThat(returned.getHeaders().getContentType().toString()).contains("application/json");
        ReturnBikeResponse body = returned.getBody();
        assertThat(body).isNotNull();
        // closed rental
        assertThat(body.closedRental()).isNotNull();
        assertThat(body.closedRental().id()).isEqualTo(rentalId);
        assertThat(body.closedRental().status()).isEqualTo("CLOSED");
        assertThat(body.closedRental().endAt()).isNotNull();
        // next rental created for waitlisted customer
        assertThat(body.nextRental()).isNotNull();
        assertThat(body.nextRental().status()).isEqualTo("ACTIVE");
        assertThat(body.nextRental().bikeId()).isEqualTo(bike.id());
        assertThat(body.nextRental().customerId()).isEqualTo(customerId2);
        // notification sent
        assertThat(body.notificationSent()).isNotNull();
        assertThat(body.notificationSent().customerId()).isEqualTo(customerId2);
        assertThat(body.notificationSent().bikeId()).isEqualTo(bike.id());
        assertThat(body.notificationSent().sentAt()).isNotNull();
    }

    @Test
    void should_list_notifications_for_customer_and_return_200() {
        BikeResponse bike = createBike(new BikeCreateRequest(
                "Bike for list notifications test",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("3.00")
        ));
        RentBikeResultResponse firstRent = rest.exchange(
                "/api/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(bike.id(), customerId1, 1), authHeaders(
                        tokenService.generateToken(
                                Objects.requireNonNull(customerRepository.findById(customerId1).orElse(null))
                        )
                )),
                RentBikeResultResponse.class
        ).getBody();
        assertThat(firstRent).isNotNull();
        Long rentalId = firstRent.rentalId();
        rest.exchange(
                "/api/rentals",
                HttpMethod.POST,
                new HttpEntity<>(new RentBikeRequest(bike.id(), customerId2, 1), authHeaders(
                        tokenService.generateToken(
                                Objects.requireNonNull(customerRepository.findById(customerId2).orElse(null))
                        )
                )),
                RentBikeResultResponse.class
        );
        rest.exchange(
                "/api/rentals/" + rentalId + "/return",
                HttpMethod.POST,
                new HttpEntity<>(new ReturnBikeRequest(customerId1, "Ok", "Good"), authHeaders(
                        tokenService.generateToken(
                                Objects.requireNonNull(customerRepository.findById(customerId1).orElse(null))
                        )
                )),
                ReturnBikeResponse.class
        );
        ResponseEntity<List<NotificationResponse>> r = rest.exchange(
                "/api/rentals/notifications?customerId=" + customerId2,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(
                        tokenService.generateToken(
                                Objects.requireNonNull(customerRepository.findById(customerId2).orElse(null))
                        )
                )),
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        List<NotificationResponse> notifications = r.getBody();
        assertThat(notifications).isNotNull();
        assertThat(notifications).isNotEmpty();
        assertThat(notifications).anySatisfy(n -> {
            assertThat(n.customerId()).isEqualTo(customerId2);
            assertThat(n.bikeId()).isEqualTo(bike.id());
        });
    }

    @Test
    void should_return_400_when_rent_request_is_invalid() {
        RentBikeRequest invalid = new RentBikeRequest(null, null, null);
        ResponseEntity<String> r = rest.exchange(
                "/api/rentals",
                HttpMethod.POST,
                new HttpEntity<>(invalid, authHeaders(
                        tokenService.generateToken(
                                Objects.requireNonNull(customerRepository.findById(customerId1).orElse(null))
                        )
                )),
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
                new HttpEntity<>(req, authHeaders(
                        tokenService.generateToken(
                                Objects.requireNonNull(customerRepository.findById(customerId1).orElse(null))
                        )
                )),
                BikeResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BikeResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        return body;
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken); // token = customer UUID
        return headers;
    }
}
