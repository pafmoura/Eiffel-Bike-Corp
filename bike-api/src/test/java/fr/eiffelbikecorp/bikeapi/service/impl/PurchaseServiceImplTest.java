package fr.eiffelbikecorp.bikeapi.service.impl;

import fr.eiffelbikecorp.bikeapi.domain.entity.Customer;
import fr.eiffelbikecorp.bikeapi.domain.entity.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.domain.enums.SaleOfferStatus;
import fr.eiffelbikecorp.bikeapi.dto.request.*;
import fr.eiffelbikecorp.bikeapi.dto.response.*;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import fr.eiffelbikecorp.bikeapi.persistence.BasketRepository;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
import fr.eiffelbikecorp.bikeapi.persistence.SaleOfferRepository;
import fr.eiffelbikecorp.bikeapi.service.*;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PurchaseServiceImplTest {

    @Autowired
    PurchaseService purchaseService;

    @Autowired
    BikeCatalogService bikeCatalogService;
    @Autowired
    RentalService rentalService;
    @Autowired
    SaleOfferService saleOfferService;
    @Autowired
    BasketService basketService;

    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    EiffelBikeCorpRepository corpRepository;
    @Autowired
    SaleOfferRepository saleOfferRepository;
    @Autowired
    BasketRepository basketRepository;

    private UUID corpId;
    private UUID customerId;
    private UUID otherCustomerId;

    @BeforeEach
    void setup() {
        EiffelBikeCorp corp = new EiffelBikeCorp();
        corp.setId(UUID.randomUUID());
        corpId = corpRepository.saveAndFlush(corp).getId();
        Customer c1 = new Customer();
        c1.setId(UUID.randomUUID());
        c1.setEmail("purchase-" + UUID.randomUUID() + "@test.com");
        c1.setFullName("Purchase Tester");
        c1.setPassword("testpassword");
        customerId = customerRepository.saveAndFlush(c1).getId();
        Customer c2 = new Customer();
        c2.setId(UUID.randomUUID());
        c2.setEmail("purchase-other-" + UUID.randomUUID() + "@test.com");
        c2.setFullName("Other Customer");
        c2.setPassword("testpassword");
        otherCustomerId = customerRepository.saveAndFlush(c2).getId();
    }

    @Test
    void should_checkout_successfully_and_close_basket() {
        // create an eligible LISTED sale offer and put it in basket
        SaleOfferResponse offer = createEligibleListedSaleOffer(new BigDecimal("199.99"));
        BasketResponse basket = basketService.addItem(customerId, new AddToBasketRequest(offer.id()));
        assertThat(basket.items()).hasSize(1);
        PurchaseResponse purchase = purchaseService.checkout(customerId);
        assertThat(purchase).isNotNull();
        assertThat(purchase.id()).isNotNull();
        assertThat(purchase.status()).isEqualTo("CREATED");
        assertThat(purchase.totalAmountEur()).isEqualByComparingTo("199.99");
        assertThat(purchase.createdAt()).isNotNull();
        assertThat(purchase.paidAt()).isNull();
        assertThat(purchase.items()).hasSize(1);
        assertThat(purchase.items().get(0).saleOfferId()).isEqualTo(offer.id());
        assertThat(purchase.items().get(0).bikeId()).isEqualTo(offer.bikeId());
        assertThat(purchase.items().get(0).unitPriceEurSnapshot()).isEqualByComparingTo("199.99");
        // Basket should have been closed
        var savedBasket = basketRepository.findById(basket.id())
                .orElseThrow(() -> new AssertionError("Basket should exist"));
        assertThat(savedBasket.getStatus().name()).isEqualTo("CHECKED_OUT");
    }

    @Test
    void should_throw_NotFoundException_when_customer_not_found() {
        UUID missing = UUID.randomUUID();
        assertThatThrownBy(() -> purchaseService.checkout(missing))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Customer not found")
                .hasMessageContaining(missing.toString());
    }

    @Test
    void should_throw_NotFoundException_when_open_basket_not_found() {
        // customer exists but we never created an OPEN basket
        assertThatThrownBy(() -> purchaseService.checkout(customerId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Open basket not found for customer")
                .hasMessageContaining(customerId.toString());
    }

    @Test
    void should_throw_BusinessRuleException_when_basket_is_empty() {
        // Create open basket but don't add items
        BasketResponse open = basketService.getOrCreateOpenBasket(customerId);
        assertThat(open.items()).isEmpty();
        assertThatThrownBy(() -> purchaseService.checkout(customerId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Basket is empty");
    }

    @Test
    void should_throw_BusinessRuleException_when_offer_is_no_longer_listed() {
        // Arrange: LISTED offer + add to basket
        SaleOfferResponse offer = createEligibleListedSaleOffer(new BigDecimal("150.00"));
        BasketResponse basket = basketService.addItem(customerId, new AddToBasketRequest(offer.id()));
        assertThat(basket.items()).hasSize(1);
        // Flip offer to SOLD after it is in basket (simulates a race condition)
        var entity = saleOfferRepository.findById(offer.id())
                .orElseThrow(() -> new AssertionError("Offer should exist"));
        entity.setStatus(SaleOfferStatus.SOLD);
        saleOfferRepository.saveAndFlush(entity);
        // Act + Assert
        assertThatThrownBy(() -> purchaseService.checkout(customerId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Offer is no longer available")
                .hasMessageContaining(offer.id().toString());
    }

    @Test
    void should_throw_NotFoundException_when_purchase_not_found() {
        assertThatThrownBy(() -> purchaseService.getPurchase(customerId, 999999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Purchase not found");
    }

    @Test
    void should_throw_BusinessRuleException_when_purchase_does_not_belong_to_customer() {
        // Arrange: create a purchase for customerId
        PurchaseResponse p = createPurchaseViaCheckout(customerId, new BigDecimal("111.00"));
        // Act + Assert: otherCustomer tries to read it
        assertThatThrownBy(() -> purchaseService.getPurchase(otherCustomerId, p.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("does not belong to customer");
    }

    @Test
    void should_list_purchases_ordered_by_createdAt_desc() throws Exception {
        PurchaseResponse p1 = createPurchaseViaCheckout(customerId, new BigDecimal("50.00"));
        Thread.sleep(5);
        PurchaseResponse p2 = createPurchaseViaCheckout(customerId, new BigDecimal("60.00"));
        List<PurchaseResponse> list = purchaseService.listPurchases(customerId);
        assertThat(list).isNotEmpty();
        assertThat(list).anySatisfy(p -> assertThat(p.id()).isEqualTo(p1.id()));
        assertThat(list).anySatisfy(p -> assertThat(p.id()).isEqualTo(p2.id()));
        // Most recent first
        PurchaseResponse first = list.get(0);
        PurchaseResponse second = list.size() > 1 ? list.get(1) : null;
        if (second != null) {
            assertThat(first.createdAt()).isAfterOrEqualTo(second.createdAt());
        }
    }

    @Test
    void should_throw_NotFoundException_when_listing_purchases_for_unknown_customer() {
        UUID missing = UUID.randomUUID();
        assertThatThrownBy(() -> purchaseService.listPurchases(missing))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Customer not found")
                .hasMessageContaining(missing.toString());
    }

    private PurchaseResponse createPurchaseViaCheckout(UUID customerId, BigDecimal askingPrice) {
        SaleOfferResponse offer = createEligibleListedSaleOffer(askingPrice);
        basketService.addItem(customerId, new AddToBasketRequest(offer.id()));
        return purchaseService.checkout(customerId);
    }

    /**
     * Creates a corporate bike, rents it once, returns it, then lists it for sale (LISTED).
     */
    private SaleOfferResponse createEligibleListedSaleOffer(BigDecimal askingPriceEur) {
        // Offer bike for rent
        BikeResponse bike = bikeCatalogService.offerBikeForRent(
                new BikeCreateRequest(
                        "Bike for purchase - " + askingPriceEur,
                        ProviderType.EIFFEL_BIKE_CORP,
                        corpId,
                        new BigDecimal("2.50")
                ),
                corpId
        );
        // Rent once
        RentBikeResultResponse rented = rentalService.rentBikeOrJoinWaitingList(
                new RentBikeRequest(bike.id(), customerId, 1)
        );
        assertThat(rented.result()).isEqualTo(RentResult.RENTED);
        // Return
        rentalService.returnBike(
                rented.rentalId(),
                new ReturnBikeRequest(customerId, "Returned OK", "GOOD")
        );
        // List for sale
        SaleOfferResponse offer = saleOfferService.createSaleOffer(
                new CreateSaleOfferRequest(bike.id(), corpId, askingPriceEur)
        );
        assertThat(offer.status()).isEqualTo("LISTED");
        return offer;
    }
}
