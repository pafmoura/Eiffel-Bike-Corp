package fr.eiffelbikecorp.bikeapi;

import fr.eiffelbikecorp.bikeapi.domain.entity.*;
import fr.eiffelbikecorp.bikeapi.domain.enums.*;
import fr.eiffelbikecorp.bikeapi.persistence.*;
import fr.eiffelbikecorp.bikeapi.security.SecurityUtils; // <--- O TEU HASH UTILS
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

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
            System.out.println(">>> DB Seeding: Exists.");
            return;
        }

        System.out.println(">>> DB Seeding Starting...");


        EiffelBikeCorp corp = new EiffelBikeCorp();
        corpRepository.save(corp);

        Student student = new Student();
        studentRepository.save(student);

        Customer alice = new Customer();
        alice.setFullName("Alice");
        alice.setEmail("alice@bike.com");

        alice.setPassword(SecurityUtils.hashSHA256("123456"));

        customerRepository.save(alice);

        Customer bob = new Customer();
        bob.setFullName("Bob");
        bob.setEmail("bob@bike.com");
        bob.setPassword(SecurityUtils.hashSHA256("123456"));
        customerRepository.save(bob);


        Bike bikeStudent = Bike.builder()
                .description("Mountain Bike Rockrider")
                .status(BikeStatus.AVAILABLE)
                .offeredBy(student)
                .rentalDailyRateEur(new BigDecimal("5.00"))
                .build();
        bikeRepository.save(bikeStudent);

        Bike bikeCorpRented = Bike.builder()
                .description("Peugeot E-Bike City")
                .status(BikeStatus.RENTED)
                .offeredBy(corp)
                .rentalDailyRateEur(new BigDecimal("15.00"))
                .build();
        bikeRepository.save(bikeCorpRented);

        Bike bikeForSale = Bike.builder()
                .description("Vintage Road Bike 1980")
                .status(BikeStatus.AVAILABLE)
                .offeredBy(corp)
                .rentalDailyRateEur(new BigDecimal("10.00"))
                .build();
        bikeRepository.save(bikeForSale);


        Rental activeRental = Rental.builder()
                .customer(alice)
                .bike(bikeCorpRented)
                .startAt(LocalDateTime.now().minusDays(2))
                .status(RentalStatus.ACTIVE)
                .totalAmountEur(new BigDecimal("30.00"))
                .build();
        rentalRepository.save(activeRental);

        Rental pastRental = Rental.builder()
                .customer(bob)
                .bike(bikeStudent)
                .startAt(LocalDateTime.now().minusDays(10))
                .endAt(LocalDateTime.now().minusDays(5))
                .status(RentalStatus.CLOSED)
                .totalAmountEur(new BigDecimal("25.00"))
                .build();

        ReturnNote note = ReturnNote.builder()
                .rental(pastRental)
                .author(bob)
                .comment("Returned without problems.")
                .condition("Good")
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();

        pastRental.setReturnNote(note);
        rentalRepository.save(pastRental);


        SaleOffer offer = SaleOffer.builder()
                .bike(bikeForSale)
                .seller(corp)
                .askingPriceEur(new BigDecimal("150.00"))
                .status(SaleOfferStatus.LISTED)
                .listedAt(LocalDateTime.now().minusHours(4))
                .notes(new ArrayList<>())
                .build();

        SaleNote saleNote = new SaleNote();
        saleNote.setSaleOffer(offer);
        saleNote.setTitle("Extra Details");
        saleNote.setContent("New Brakes.");
        saleNote.setCreatedBy("Admin");
        saleNote.setCreatedAt(LocalDateTime.now());

        offer.getNotes().add(saleNote);
        saleOfferRepository.save(offer);

        // ==========================================
        // 5. WAITING LIST & BASKET
        // ==========================================

        WaitingList waitingList = new WaitingList();
        waitingList.setBike(bikeCorpRented);
        waitingList.setEntries(new ArrayList<>());

        WaitingListEntry entry = new WaitingListEntry();
        entry.setWaitingList(waitingList);
        entry.setCustomer(bob);
        entry.setCreatedAt(LocalDateTime.now());

        waitingList.getEntries().add(entry);
        waitingListRepository.save(waitingList);

        Basket basket = Basket.builder()
                .customer(alice)
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

        System.out.println(">>> DB Seeding: Concluded!.");
    }
}