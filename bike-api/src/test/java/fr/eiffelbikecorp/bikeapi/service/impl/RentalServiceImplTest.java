package fr.eiffelbikecorp.bikeapi.service.impl;

import fr.eiffelbikecorp.bikeapi.domain.entity.Customer;
import fr.eiffelbikecorp.bikeapi.domain.entity.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.RentBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.ReturnBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.RentBikeResultResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.ReturnBikeResponse;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
import fr.eiffelbikecorp.bikeapi.service.BikeCatalogService;
import fr.eiffelbikecorp.bikeapi.service.RentalService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RentalServiceImplTest {

    @Autowired
    RentalService rentalService;
    @Autowired
    BikeCatalogService bikeCatalogService;

    @Autowired
    EiffelBikeCorpRepository corpRepository;
    @Autowired
    CustomerRepository customerRepository;

    private UUID corpId;
    private UUID customerId1;
    private UUID customerId2;

    @BeforeEach
    void setup() {
        // Corporate provider
        EiffelBikeCorp corp = new EiffelBikeCorp();
        corpId = corpRepository.saveAndFlush(corp).getId();
        // Customer 1
        Customer c1 = new Customer();
        c1.setEmail(randomEmail());
        c1.setFullName("John Customer 1");
        c1.setPassword("testpassword");
        customerId1 = customerRepository.saveAndFlush(c1).getId();
        // Customer 2
        Customer c2 = new Customer();
        c2.setEmail(randomEmail());
        c2.setFullName("John Customer 2");
        c2.setPassword("testpassword");
        customerId2 = customerRepository.saveAndFlush(c2).getId();
    }

    @Test
    void should_rent_bike_when_available_and_return_RENTED() {
        BikeResponse bike = offerBike();
        RentBikeRequest rentReq = new RentBikeRequest(
                bike.id(),
                customerId1,
                3
        );
        RentBikeResultResponse r = rentalService.rentBikeOrJoinWaitingList(rentReq);
        assertThat(r).isNotNull();
        assertThat(r.result()).isEqualTo(RentResult.RENTED);
        assertThat(r.rentalId()).isNotNull();
        assertThat(r.waitingListEntryId()).isNull();
        assertThat(r.message()).containsIgnoringCase("rented");
    }

    @Test
    void should_join_waiting_list_when_bike_unavailable_and_return_WAITLISTED() {
        BikeResponse bike = offerBike();
        RentBikeResultResponse first = rentalService.rentBikeOrJoinWaitingList(
                new RentBikeRequest(bike.id(), customerId1, 2)
        );
        assertThat(first.result()).isEqualTo(RentResult.RENTED);
        RentBikeResultResponse second = rentalService.rentBikeOrJoinWaitingList(
                new RentBikeRequest(bike.id(), customerId2, 2)
        );
        assertThat(second.result()).isEqualTo(RentResult.WAITLISTED);
        assertThat(second.rentalId()).isNull();
        assertThat(second.waitingListEntryId()).isNotNull();
        assertThat(second.message()).containsIgnoringCase("waiting list");
    }

    @Test
    void should_throw_BusinessRuleException_when_customer_already_in_waiting_list_for_same_bike() {
        BikeResponse bike = offerBike();
        rentalService.rentBikeOrJoinWaitingList(new RentBikeRequest(bike.id(), customerId1, 1));
        RentBikeResultResponse waitlisted = rentalService.rentBikeOrJoinWaitingList(
                new RentBikeRequest(bike.id(), customerId2, 1)
        );
        assertThat(waitlisted.result()).isEqualTo(RentResult.WAITLISTED);
        assertThatThrownBy(() ->
                rentalService.rentBikeOrJoinWaitingList(new RentBikeRequest(bike.id(), customerId2, 1))
        )
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already in the waiting list");
    }

    @Test
    void should_return_bike_and_auto_rent_to_next_waitlisted_customer_and_send_notification() {
        BikeResponse bike = offerBike();
        RentBikeResultResponse rent1 = rentalService.rentBikeOrJoinWaitingList(
                new RentBikeRequest(bike.id(), customerId1, 2)
        );
        assertThat(rent1.result()).isEqualTo(RentResult.RENTED);
        Long rentalId1 = rent1.rentalId();
        RentBikeResultResponse wait2 = rentalService.rentBikeOrJoinWaitingList(
                new RentBikeRequest(bike.id(), customerId2, 2)
        );
        assertThat(wait2.result()).isEqualTo(RentResult.WAITLISTED);
        ReturnBikeResponse returned = rentalService.returnBike(
                rentalId1,
                new ReturnBikeRequest(
                        customerId1,
                        "Bike ok, small scratch",
                        "GOOD"
                )
        );
        assertThat(returned).isNotNull();
        assertThat(returned.closedRental()).isNotNull();
        assertThat(returned.closedRental().id()).isEqualTo(rentalId1);
        assertThat(returned.closedRental().status()).isEqualTo("CLOSED");
        // Next rental created for customer2
        assertThat(returned.nextRental()).isNotNull();
        assertThat(returned.nextRental().customerId()).isEqualTo(customerId2);
        assertThat(returned.nextRental().bikeId()).isEqualTo(bike.id());
        assertThat(returned.nextRental().status()).isEqualTo("ACTIVE");
        // Notification sent to customer2
        assertThat(returned.notificationSent()).isNotNull();
        assertThat(returned.notificationSent().customerId()).isEqualTo(customerId2);
        assertThat(returned.notificationSent().bikeId()).isEqualTo(bike.id());
        assertThat(returned.notificationSent().message()).isNotBlank();
    }

    @Test
    void should_throw_NotFoundException_when_renting_with_unknown_customer() {
        BikeResponse bike = offerBike();
        UUID missingCustomerId = UUID.randomUUID();
        assertThatThrownBy(() ->
                rentalService.rentBikeOrJoinWaitingList(new RentBikeRequest(bike.id(), missingCustomerId, 1))
        )
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void should_throw_NotFoundException_when_renting_unknown_bike() {
        Long missingBikeId = 999999L;
        assertThatThrownBy(() ->
                rentalService.rentBikeOrJoinWaitingList(new RentBikeRequest(missingBikeId, customerId1, 1))
        )
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Bike not found");
    }

    private BikeResponse offerBike() {
        BikeCreateRequest req = new BikeCreateRequest(
                "Bike for rental tests",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.50")
        );
        return bikeCatalogService.offerBikeForRent(req, req.offeredById());
    }

    private static String randomEmail() {
        return "user-" + UUID.randomUUID() + "@bike.com";
    }
}
