package fr.eiffelbikecorp.bikeapi.service.impl;

import fr.eiffelbikecorp.bikeapi.domain.entity.Customer;
import fr.eiffelbikecorp.bikeapi.domain.entity.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.domain.entity.SaleOffer;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.domain.enums.SaleOfferStatus;
import fr.eiffelbikecorp.bikeapi.dto.request.*;
import fr.eiffelbikecorp.bikeapi.dto.response.*;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
import fr.eiffelbikecorp.bikeapi.persistence.SaleOfferRepository;
import fr.eiffelbikecorp.bikeapi.service.BasketService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BasketServiceImplTest {

    @Autowired
    BasketService basketService;

    @Autowired
    BikeCatalogService bikeCatalogService;
    @Autowired
    RentalService rentalService;
    @Autowired
    SaleOfferService saleOfferService;

    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    EiffelBikeCorpRepository corpRepository;
    @Autowired
    SaleOfferRepository saleOfferRepository;

    private UUID customerId;
    private UUID corpId;

    @BeforeEach
    void setup() {
        EiffelBikeCorp corp = new EiffelBikeCorp();
        corpId = corpRepository.saveAndFlush(corp).getId();
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setEmail("basket-svc-" + UUID.randomUUID() + "@test.com");
        c.setFullName("Basket Service Tester");
        c.setPassword("testpassword");
        customerId = customerRepository.saveAndFlush(c).getId();
    }

    @Test
    void should_get_or_create_open_basket_and_return_empty_items() {
        BasketResponse b1 = basketService.getOrCreateOpenBasket(customerId);
        assertThat(b1).isNotNull();
        assertThat(b1.id()).isNotNull();
        assertThat(b1.status()).isEqualTo("OPEN");
        assertThat(b1.createdAt()).isNotNull();
        assertThat(b1.updatedAt()).isNotNull();
        assertThat(b1.items()).isNotNull();
        assertThat(b1.items()).isEmpty();
        // calling again should return the same open basket
        BasketResponse b2 = basketService.getOrCreateOpenBasket(customerId);
        assertThat(b2.id()).isEqualTo(b1.id());
    }

    @Test
    void should_add_item_and_create_open_basket_if_missing() {
        SaleOfferResponse offer = createEligibleSaleOffer("Bike for basket add", new BigDecimal("199.99"));
        BasketResponse basket = basketService.addItem(customerId, new AddToBasketRequest(offer.id()));
        assertThat(basket).isNotNull();
        assertThat(basket.id()).isNotNull();
        assertThat(basket.status()).isEqualTo("OPEN");
        assertThat(basket.items()).hasSize(1);
        BasketItemResponse item = basket.items().get(0);
        assertThat(item.id()).isNotNull();
        assertThat(item.saleOfferId()).isEqualTo(offer.id());
        assertThat(item.bikeId()).isEqualTo(offer.bikeId());
        assertThat(item.unitPriceEurSnapshot()).isEqualByComparingTo("199.99");
        assertThat(item.addedAt()).isNotNull();
    }

    @Test
    void should_throw_NotFoundException_when_customer_does_not_exist() {
        UUID missingCustomerId = UUID.randomUUID();
        assertThatThrownBy(() -> basketService.getOrCreateOpenBasket(missingCustomerId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Customer not found");
        assertThatThrownBy(() -> basketService.addItem(missingCustomerId, new AddToBasketRequest(1L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void should_throw_NotFoundException_when_sale_offer_does_not_exist() {
        basketService.getOrCreateOpenBasket(customerId);
        assertThatThrownBy(() -> basketService.addItem(customerId, new AddToBasketRequest(999999L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("SaleOffer not found");
    }

    @Test
    void should_throw_BusinessRuleException_when_offer_is_not_listed() {
        SaleOfferResponse offer = createEligibleSaleOffer("Bike for not listed", new BigDecimal("150.00"));
        // Force offer status to SOLD so BasketService rejects it
        SaleOffer persisted = saleOfferRepository.findById(offer.id())
                .orElseThrow(() -> new AssertionError("Offer should exist"));
        persisted.setStatus(SaleOfferStatus.SOLD);
        saleOfferRepository.saveAndFlush(persisted);
        assertThatThrownBy(() -> basketService.addItem(customerId, new AddToBasketRequest(offer.id())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Offer is not available");
    }

    @Test
    void should_throw_BusinessRuleException_when_offer_already_in_basket() {
        SaleOfferResponse offer = createEligibleSaleOffer("Bike duplicated", new BigDecimal("120.00"));
        BasketResponse first = basketService.addItem(customerId, new AddToBasketRequest(offer.id()));
        assertThat(first.items()).hasSize(1);
        assertThatThrownBy(() -> basketService.addItem(customerId, new AddToBasketRequest(offer.id())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Offer already in basket");
    }

    @Test
    void should_remove_item_from_open_basket() {
        SaleOfferResponse offer = createEligibleSaleOffer("Bike remove", new BigDecimal("175.00"));
        BasketResponse withItem = basketService.addItem(customerId, new AddToBasketRequest(offer.id()));
        assertThat(withItem.items()).hasSize(1);
        BasketResponse afterRemove = basketService.removeItem(customerId, offer.id());
        assertThat(afterRemove.items()).isEmpty();
    }

    @Test
    void should_throw_NotFoundException_when_removing_item_without_open_basket() {
        // customer exists but no OPEN basket yet
        assertThatThrownBy(() -> basketService.removeItem(customerId, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Open basket not found");
    }

    @Test
    void should_throw_NotFoundException_when_item_not_found_in_basket() {
        // create empty OPEN basket
        BasketResponse b = basketService.getOrCreateOpenBasket(customerId);
        assertThat(b.items()).isEmpty();
        assertThatThrownBy(() -> basketService.removeItem(customerId, 123456L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Item not found in basket");
    }

    @Test
    void should_clear_open_basket_items() {
        SaleOfferResponse offer = createEligibleSaleOffer("Bike clear", new BigDecimal("99.00"));
        BasketResponse withItem = basketService.addItem(customerId, new AddToBasketRequest(offer.id()));
        assertThat(withItem.items()).hasSize(1);
        BasketResponse cleared = basketService.clear(customerId);
        assertThat(cleared.items()).isEmpty();
    }

    @Test
    void should_throw_NotFoundException_when_clearing_without_open_basket() {
        assertThatThrownBy(() -> basketService.clear(customerId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Open basket not found for customer");
    }

    private SaleOfferResponse createEligibleSaleOffer(String bikeDescription, BigDecimal askingPriceEur) {
        BikeResponse bike = bikeCatalogService.offerBikeForRent(
                new BikeCreateRequest(
                        bikeDescription,
                        ProviderType.EIFFEL_BIKE_CORP,
                        corpId,
                        new BigDecimal("2.50")
                ),
                corpId
        );
        // eligibility rule: must be rented at least once and bike should be available again
        RentBikeResultResponse rented = rentalService.rentBikeOrJoinWaitingList(
                new RentBikeRequest(bike.id(), customerId, 1)
        );
        assertThat(rented.result()).isEqualTo(RentResult.RENTED);
        assertThat(rented.rentalId()).isNotNull();
        rentalService.returnBike(
                rented.rentalId(),
                new ReturnBikeRequest(customerId, "Returned OK", "Good")
        );
        SaleOfferResponse offer = saleOfferService.createSaleOffer(
                new CreateSaleOfferRequest(bike.id(), corpId, askingPriceEur)
        );
        assertThat(offer).isNotNull();
        assertThat(offer.id()).isNotNull();
        assertThat(offer.status()).isEqualTo("LISTED");
        return offer;
    }
}
