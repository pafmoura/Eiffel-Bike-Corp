package fr.eiffelbikecorp.bikeapi.service.impl;

import fr.eiffelbikecorp.bikeapi.domain.entity.Customer;
import fr.eiffelbikecorp.bikeapi.domain.entity.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.domain.entity.SaleOffer;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.domain.enums.SaleOfferStatus;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.CreateSaleNoteRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.CreateSaleOfferRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.RentBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.*;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
import fr.eiffelbikecorp.bikeapi.persistence.SaleOfferRepository;
import fr.eiffelbikecorp.bikeapi.service.BikeCatalogService;
import fr.eiffelbikecorp.bikeapi.service.RentalService;
import fr.eiffelbikecorp.bikeapi.service.SaleOfferService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SaleOfferServiceImplTest{

    @Autowired
    SaleOfferService saleOfferService;
    @Autowired
    BikeCatalogService bikeCatalogService;
    @Autowired
    RentalService rentalService;

    @Autowired
    EiffelBikeCorpRepository corpRepository;
    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    SaleOfferRepository saleOfferRepository;

    private UUID corpId;
    private UUID customerId;

    @BeforeEach
    void setup() {
        EiffelBikeCorp corp = new EiffelBikeCorp();
        corpId = corpRepository.saveAndFlush(corp).getId();
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setEmail("sale-service-" + UUID.randomUUID() + "@test.com");
        c.setFullName("Buyer");
        c.setPassword("testpassword");
        customerId = customerRepository.saveAndFlush(c).getId();
    }

    @Test
    void should_create_sale_offer_when_bike_was_rented_at_least_once() {
        BikeResponse bike = offerCorpBike("Corporate bike - used once");
        rentBike(bike.id(), customerId);
        CreateSaleOfferRequest req = new CreateSaleOfferRequest(
                bike.id(),
                corpId,
                new BigDecimal("250.00")
        );
        SaleOfferResponse resp = saleOfferService.createSaleOffer(req);
        assertThat(resp).isNotNull();
        assertThat(resp.id()).isNotNull();
        assertThat(resp.bikeId()).isEqualTo(bike.id());
        assertThat(resp.status()).isEqualTo("LISTED");
        assertThat(resp.askingPriceEur()).isEqualByComparingTo("250.00");
        assertThat(resp.listedAt()).isNotNull();
        assertThat(resp.soldAt()).isNull();
        assertThat(resp.availability()).isNotBlank();
    }

    @Test
    void should_throw_BusinessRuleException_when_bike_was_never_rented() {
        BikeResponse bike = offerCorpBike("Corporate bike - never rented");
        assertThatThrownBy(() -> saleOfferService.createSaleOffer(
                new CreateSaleOfferRequest(bike.id(), corpId, new BigDecimal("199.00"))
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("rented at least once");
    }

    @Test
    void should_throw_BusinessRuleException_when_sale_offer_already_exists_for_bike() {
        BikeResponse bike = offerCorpBike("Corporate bike - offer duplication");
        rentBike(bike.id(), customerId);
        SaleOfferResponse first = saleOfferService.createSaleOffer(
                new CreateSaleOfferRequest(bike.id(), corpId, new BigDecimal("180.00"))
        );
        assertThat(first.status()).isEqualTo("LISTED");
        assertThatThrownBy(() -> saleOfferService.createSaleOffer(
                new CreateSaleOfferRequest(bike.id(), corpId, new BigDecimal("175.00"))
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already has a sale offer");
    }

    @Test
    void should_throw_BusinessRuleException_when_seller_is_not_owner_of_bike() {
        BikeResponse bike = offerCorpBike("Corporate bike - wrong seller");
        rentBike(bike.id(), customerId);
        UUID someoneElse = UUID.randomUUID();
        assertThatThrownBy(() -> saleOfferService.createSaleOffer(
                new CreateSaleOfferRequest(bike.id(), someoneElse, new BigDecimal("220.00"))
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("only list your own bikes");
    }

    @Test
    void should_throw_NotFoundException_when_creating_sale_offer_for_unknown_bike() {
        assertThatThrownBy(() -> saleOfferService.createSaleOffer(
                new CreateSaleOfferRequest(999999L, corpId, new BigDecimal("200.00"))
        ))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Bike not found");
    }

    @Test
    void should_add_sale_note_and_return_details_with_notes() {
        BikeResponse bike = offerCorpBike("Corporate bike - notes");
        rentBike(bike.id(), customerId);
        SaleOfferResponse offer = saleOfferService.createSaleOffer(
                new CreateSaleOfferRequest(bike.id(), corpId, new BigDecimal("210.00"))
        );
        SaleNoteResponse note = saleOfferService.addSaleNote(new CreateSaleNoteRequest(
                offer.id(),
                "Condition",
                "Minor scratches, fully functional.",
                "EiffelBikeCorp"
        ));
        assertThat(note).isNotNull();
        assertThat(note.id()).isNotNull();
        assertThat(note.saleOfferId()).isEqualTo(offer.id());
        assertThat(note.title()).isEqualTo("Condition");
        assertThat(note.content()).contains("scratches");
        assertThat(note.createdAt()).isNotNull();
        SaleOfferDetailsResponse details = saleOfferService.getSaleOfferDetails(offer.id());
        assertThat(details).isNotNull();
        assertThat(details.offer()).isNotNull();
        assertThat(details.offer().id()).isEqualTo(offer.id());
        assertThat(details.notes()).isNotNull();
        assertThat(details.notes()).isNotEmpty();
        assertThat(details.notes()).anySatisfy(n -> {
            assertThat(n.title()).isEqualTo("Condition");
            assertThat(n.content()).contains("functional");
        });
    }

    @Test
    void should_throw_BusinessRuleException_when_adding_note_to_non_listed_offer() {
        BikeResponse bike = offerCorpBike("Corporate bike - sold");
        rentBike(bike.id(), customerId);
        SaleOfferResponse offerResp = saleOfferService.createSaleOffer(
                new CreateSaleOfferRequest(bike.id(), corpId, new BigDecimal("300.00"))
        );
        SaleOffer offer = saleOfferRepository.findById(offerResp.id())
                .orElseThrow(() -> new AssertionError("Offer should exist"));
        offer.setStatus(SaleOfferStatus.SOLD);
        offer.setSoldAt(LocalDateTime.now());
        saleOfferRepository.saveAndFlush(offer);
        assertThatThrownBy(() -> saleOfferService.addSaleNote(new CreateSaleNoteRequest(
                offerResp.id(),
                "After sale note",
                "Should not be allowed",
                "System"
        )))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not LISTED");
    }

    @Test
    void should_search_sale_offers_with_and_without_query() {
        BikeResponse redBike = offerCorpBike("Red commuter bike");
        rentBike(redBike.id(), customerId);
        SaleOfferResponse redOffer = saleOfferService.createSaleOffer(
                new CreateSaleOfferRequest(redBike.id(), corpId, new BigDecimal("120.00"))
        );
        BikeResponse blueBike = offerCorpBike("Blue city bike");
        rentBike(blueBike.id(), customerId);
        SaleOfferResponse blueOffer = saleOfferService.createSaleOffer(
                new CreateSaleOfferRequest(blueBike.id(), corpId, new BigDecimal("130.00"))
        );
        List<SaleOfferResponse> all = saleOfferService.searchSaleOffers(null);
        assertThat(all).isNotEmpty();
        assertThat(all).anySatisfy(o -> assertThat(o.id()).isEqualTo(redOffer.id()));
        assertThat(all).anySatisfy(o -> assertThat(o.id()).isEqualTo(blueOffer.id()));
        List<SaleOfferResponse> redOnly = saleOfferService.searchSaleOffers("Red");
        assertThat(redOnly).anySatisfy(o -> assertThat(o.id()).isEqualTo(redOffer.id()));
        assertThat(redOnly).noneSatisfy(o -> assertThat(o.id()).isEqualTo(blueOffer.id()));
    }

    @Test
    void should_throw_NotFoundException_when_getting_details_for_unknown_offer() {
        assertThatThrownBy(() -> saleOfferService.getSaleOfferDetails(999999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("SaleOffer not found");
    }

    @Test
    void should_throw_NotFoundException_when_getting_details_by_unknown_bike() {
        assertThatThrownBy(() -> saleOfferService.getSaleOfferDetailsByBike(999999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("SaleOffer not found for bike");
    }

    private BikeResponse offerCorpBike(String description) {
        BikeCreateRequest req = new BikeCreateRequest(
                description,
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.50")
        );
        return bikeCatalogService.offerBikeForRent(req, req.offeredById());
    }

    private void rentBike(Long bikeId, UUID customerId) {
        RentBikeResultResponse r = rentalService.rentBikeOrJoinWaitingList(
                new RentBikeRequest(bikeId, customerId, 1)
        );
        assertThat(r.result()).isEqualTo(RentResult.RENTED);
        assertThat(r.rentalId()).isNotNull();
    }
}
