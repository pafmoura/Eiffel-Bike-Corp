package fr.eiffelbikecorp.bikeapi;

import fr.eiffelbikecorp.bikeapi.domain.entity.*;
import fr.eiffelbikecorp.bikeapi.domain.enums.BasketStatus;
import fr.eiffelbikecorp.bikeapi.domain.enums.BikeStatus;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentalStatus;
import fr.eiffelbikecorp.bikeapi.domain.enums.SaleOfferStatus;
import fr.eiffelbikecorp.bikeapi.persistence.*;
import fr.eiffelbikecorp.bikeapi.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class DatabaseSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final StudentRepository studentRepository;
    private final EiffelBikeCorpRepository corpRepository;
    private final BikeRepository bikeRepository;
    private final RentalRepository rentalRepository;
    private final SaleOfferRepository saleOfferRepository;
    private final WaitingListRepository waitingListRepository;
    private final BasketRepository basketRepository;
    private final BasketItemRepository basketItemRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (customerRepository.count() > 0) {
            System.out.println(">>> DB Seeding: Data already exists. Skipping.");
            return;
        }
        System.out.println(">>> DB Seeding Starting...");
        // ==========================================
        // 1. CREATE PROVIDERS & USERS
        // ==========================================
        // --- 1b. Alice (The Student Provider) ---
        // Alice needs two records:
        // 1. A Customer record (to login)
        // 2. A Student record (to provide bikes)
        // They MUST share the same UUID.
        UUID aliceId = UUID.randomUUID();
        // Alice - The User/Login part
        Customer aliceUser = new Customer();
        aliceUser.setId(aliceId);
        aliceUser.setFullName("Alice Student");
        aliceUser.setEmail("alice@bike.com");
        aliceUser.setPassword(SecurityUtils.hashSHA256("123456"));
        customerRepository.save(aliceUser);
        // Alice - The Provider part
        Student aliceProvider = new Student();
        aliceProvider.setId(aliceId); // Critical: Same ID as Customer
        studentRepository.save(aliceProvider);
        // --- 1c. Bob (The Standard Customer) ---
        Customer bobUser = new Customer();
        bobUser.setId(UUID.randomUUID());
        bobUser.setFullName("Bob Customer");
        bobUser.setEmail("bob@bike.com");
        bobUser.setPassword(SecurityUtils.hashSHA256("123456"));
        customerRepository.save(bobUser);
        UUID adminId = UUID.randomUUID();
        Customer adminUser = new Customer();
        adminUser.setId(adminId);
        adminUser.setFullName("Admin User");
        adminUser.setEmail("admin@bike.com");
        adminUser.setPassword(SecurityUtils.hashSHA256("123456"));
        customerRepository.save(adminUser);
        EiffelBikeCorp corp = new EiffelBikeCorp();
        corp.setId(adminId);
        corpRepository.save(corp);
        // ==========================================
        // 2. CREATE BIKES
        // ==========================================
        // Bike 1: Owned by Alice (The Student)
        Bike bikeStudent = Bike.builder()
                .description("Mountain Bike Rockrider")
                .status(BikeStatus.AVAILABLE)
                .offeredBy(aliceProvider) // Alice is the provider
                .rentalDailyRateEur(new BigDecimal("5.00"))
                .build();
        bikeRepository.save(bikeStudent);
        // Bike 2: Owned by Corp (Rented out)
        Bike bikeCorpRented = Bike.builder()
                .description("Peugeot E-Bike City")
                .status(BikeStatus.RENTED)
                .offeredBy(corp)
                .rentalDailyRateEur(new BigDecimal("15.00"))
                .build();
        bikeRepository.save(bikeCorpRented);
        // Bike 3: Owned by Corp (For Sale)
        Bike bikeForSale = Bike.builder()
                .description("Vintage Road Bike 1980")
                .status(BikeStatus.AVAILABLE)
                .offeredBy(corp)
                .rentalDailyRateEur(new BigDecimal("10.00"))
                .build();
        bikeRepository.save(bikeForSale);
        // ==========================================
        // 3. CREATE RENTALS
        // ==========================================
        // Alice rents the Corp's E-Bike
        Rental activeRental = Rental.builder()
                .customer(aliceUser)
                .bike(bikeCorpRented)
                .startAt(LocalDateTime.now().minusDays(2))
                .status(RentalStatus.ACTIVE)
                .totalAmountEur(new BigDecimal("30.00"))
                .build();
        rentalRepository.save(activeRental);
        // Bob rented Alice's Mountain Bike (Closed now)
        Rental pastRental = Rental.builder()
                .customer(bobUser)
                .bike(bikeStudent)
                .startAt(LocalDateTime.now().minusDays(10))
                .endAt(LocalDateTime.now().minusDays(5))
                .status(RentalStatus.CLOSED)
                .totalAmountEur(new BigDecimal("25.00"))
                .build();
        // Bob left a note
        ReturnNote note = ReturnNote.builder()
                .rental(pastRental)
                .author(bobUser)
                .comment("Returned without problems. Great bike!")
                .condition("Good")
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();
        pastRental.setReturnNote(note);
        rentalRepository.save(pastRental);
        // ==========================================
        // 4. SALE OFFERS
        // ==========================================
        SaleOffer offer = SaleOffer.builder()
                .bike(bikeForSale)
                .sellerId(bobUser.getId())
                .askingPriceEur(new BigDecimal("150.00"))
                .status(SaleOfferStatus.LISTED)
                .listedAt(LocalDateTime.now().minusHours(4))
                .build();
        SaleNote saleNote = new SaleNote();
        saleNote.setSaleOffer(offer);
        saleNote.setTitle("Extra Details");
        saleNote.setContent("New Brakes installed by mechanics.");
        saleNote.setCreatedBy("Admin");
        saleNote.setCreatedAt(LocalDateTime.now());
        offer.getNotes().add(saleNote);
        saleOfferRepository.save(offer);
        // ==========================================
        // 5.  BASKET
        // ==========================================

        // Alice has the Vintage Bike offer in her basket
        Basket basket = Basket.builder()
                .customer(aliceUser)
                .status(BasketStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();
        basketRepository.save(basket);
        BasketItem item = new BasketItem();
        item.setBasket(basket);
        item.setOffer(offer);
        item.setUnitPriceEurSnapshot(offer.getAskingPriceEur());
        item.setAddedAt(LocalDateTime.now());
        basketItemRepository.save(item);
        System.out.println(">>> DB Seeding: Concluded successfully!");
    }
}