package fr.eiffelbikecorp.bikeapi.service.impl;

import fr.eiffelbikecorp.bikeapi.domain.entity.*;
import fr.eiffelbikecorp.bikeapi.domain.enums.BikeStatus;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentResult;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentalStatus;
import fr.eiffelbikecorp.bikeapi.dto.request.RentBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.ReturnBikeRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.*;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import fr.eiffelbikecorp.bikeapi.mapper.NotificationMapper;
import fr.eiffelbikecorp.bikeapi.mapper.RentalMapper;
import fr.eiffelbikecorp.bikeapi.persistence.*;
import fr.eiffelbikecorp.bikeapi.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RentalServiceImpl implements RentalService {

    private final BikeRepository bikeRepository;
    private final CustomerRepository customerRepository;
    private final RentalRepository rentalRepository;

    private final WaitingListRepository waitingListRepository;
    private final WaitingListEntryRepository waitingListEntryRepository;

    private final ReturnNoteRepository returnNoteRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public RentBikeResultResponse rentBikeOrJoinWaitingList(RentBikeRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Customer not found: " + request.customerId()));
        Bike bike = bikeRepository.findByIdForUpdate(request.bikeId())
                .orElseThrow(() -> new NotFoundException("Bike not found: " + request.bikeId()));
        boolean hasActiveRental = rentalRepository.existsByBike_IdAndStatus(bike.getId(), RentalStatus.ACTIVE);
        if (bike.getStatus() == BikeStatus.AVAILABLE && !hasActiveRental) {
            var now = LocalDateTime.now();
            BigDecimal total = bike.getRentalDailyRateEur()
                    .multiply(BigDecimal.valueOf(request.days()));
            Rental rental = Rental.builder()
                    .bike(bike)
                    .customer(customer)
                    .startAt(now)
                    .endAt(null)
                    .status(RentalStatus.ACTIVE)
                    .totalAmountEur(total)
                    .build();
            bike.setStatus(BikeStatus.RENTED);
            Rental saved = rentalRepository.save(rental);
            bikeRepository.save(bike);
            return new RentBikeResultResponse(
                    RentResult.RENTED,
                    saved.getId(),
                    null,
                    "Bike rented successfully."
            );
        }
        // join waiting
        WaitingList waitingList = waitingListRepository.findByBike_Id(bike.getId())
                .orElseGet(() -> waitingListRepository.save(
                        WaitingList.builder().bike(bike).build()
                ));
        if (waitingListEntryRepository.existsByWaitingList_IdAndCustomer_IdAndServedAtIsNull(waitingList.getId(), customer.getId())) {
            throw new BusinessRuleException("Customer is already in the waiting list for this bike.");
        }
        WaitingListEntry entry = WaitingListEntry.builder()
                .waitingList(waitingList)
                .customer(customer)
                .createdAt(LocalDateTime.now())
                .build();
        WaitingListEntry savedEntry = waitingListEntryRepository.save(entry);
        return new RentBikeResultResponse(
                RentResult.WAITLISTED,
                null,
                savedEntry.getId(),
                "Bike not available. You have been added to the waiting list."
        );
    }

    @Override
    @Transactional
    public ReturnBikeResponse returnBike(Long rentalId, ReturnBikeRequest request) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new NotFoundException("Rental not found: " + rentalId));
        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new BusinessRuleException("Only ACTIVE rentals can be returned.");
        }
        Customer author = customerRepository.findById(request.authorCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer (author) not found: " + request.authorCustomerId()));
        Bike bike = bikeRepository.findByIdForUpdate(rental.getBike().getId())
                .orElseThrow(() -> new NotFoundException("Bike not found: " + rental.getBike().getId()));
        rental.setStatus(RentalStatus.CLOSED);
        rental.setEndAt(LocalDateTime.now());
        Rental closed = rentalRepository.save(rental);
        if (returnNoteRepository.existsByRental_Id(rentalId)) {
            throw new BusinessRuleException("A return note already exists for this rental.");
        }
        ReturnNote note = ReturnNote.builder()
                .rental(closed)
                .author(author)
                .comment(request.comment())
                .condition(request.condition())
                .createdAt(LocalDateTime.now())
                .build();
        returnNoteRepository.save(note);
        // Mark bike available first
        waitingListRepository.findByBike_Id(bike.getId()).ifPresent(wl -> {
            waitingListEntryRepository.findByWaitingList_IdAndCustomer_Id(wl.getId(), rental.getCustomer().getId())
                    .ifPresent(entry -> {
                        if (entry.getServedAt() == null) {
                            entry.setServedAt(LocalDateTime.now());
                            waitingListEntryRepository.save(entry);
                        }
                    });
        });
        bike.setStatus(BikeStatus.AVAILABLE);
        bikeRepository.save(bike);
        // FIFO waiting list: assign next automatically + notify
        var maybeWaitingList = waitingListRepository.findByBike_Id(bike.getId());
        if (maybeWaitingList.isEmpty()) {
            return new ReturnBikeResponse(
                    RentalMapper.toResponse(closed),
                    null,
                    null
            );
        }
        WaitingList wl = maybeWaitingList.get();
        var nextEntryOpt = waitingListEntryRepository.findFirstByWaitingList_IdAndServedAtIsNullOrderByCreatedAtAsc(wl.getId());
        if (nextEntryOpt.isEmpty()) {
            return new ReturnBikeResponse(
                    RentalMapper.toResponse(closed),
                    null,
                    null
            );
        }
        WaitingListEntry nextEntry = nextEntryOpt.get();
        // Create next rental automatically (requirement: "notified and rents the bike")
        Customer nextCustomer = nextEntry.getCustomer();
        Rental nextRental = Rental.builder()
                .bike(bike)
                .customer(nextCustomer)
                .startAt(LocalDateTime.now())
                .endAt(null)
                .status(RentalStatus.ACTIVE)
                .totalAmountEur(bike.getRentalDailyRateEur())
                .build();
        bike.setStatus(BikeStatus.RENTED);
        Rental savedNextRental = rentalRepository.save(nextRental);
        bikeRepository.save(bike);
        // mark as served
        nextEntry.setServedAt(LocalDateTime.now());
        waitingListEntryRepository.save(nextEntry);
        Notification notification = Notification.builder()
                .entry(nextEntry)
                .message("Bike " + bike.getId() + " is now available. A rental has been created for you.")
                .sentAt(LocalDateTime.now())
                .build();
        Notification savedNotification = notificationRepository.save(notification);
        return new ReturnBikeResponse(
                RentalMapper.toResponse(closed),
                RentalMapper.toResponse(savedNextRental),
                NotificationMapper.toResponse(savedNotification)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> listMyNotifications(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new NotFoundException("Customer not found: " + customerId);
        }
        return notificationRepository.findByEntry_Customer_IdOrderBySentAtDesc(customerId)
                .stream()
                .map(NotificationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentBikeResultResponse> findActiveRentalsByCustomer(UUID customerId) {
        return rentalRepository.findByCustomer_IdAndStatusIn(customerId, List.of(RentalStatus.ACTIVE))
                .stream()
                .map(rental -> new RentBikeResultResponse(
                        RentResult.RENTED,
                        rental.getId(),
                        null,
                        rental.getBike().getDescription()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActiveBikeResponse> findMyActiveBikeIds(UUID customerId) {
        return rentalRepository.findByCustomer_IdAndStatusIn(customerId, List.of(RentalStatus.ACTIVE))
                .stream()
                .map(r -> new ActiveBikeResponse(r.getBike().getId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> findWaitlistByCustomer(UUID customerId) {
        return waitingListEntryRepository.findByCustomer_IdAndServedAtIsNull(customerId)
                .stream()
                .map(entry -> new NotificationResponse(
                        entry.getId(),                                     // Long id
                        entry.getCustomer().getId(),                       // UUID customerId
                        entry.getWaitingList().getBike().getId(),          // Long bikeId
                        "Waiting for " + entry.getWaitingList().getBike().getDescription(), // String message
                        entry.getCreatedAt()                               // LocalDateTime (using createdAt as sentAt)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<RentalResponse> listMyRentals(UUID customerId) {
        return rentalRepository.findByCustomer_IdOrderByStartAtDesc(customerId).stream()
                .map(r -> new RentalResponse(
                        r.getId(),
                        r.getBike().getId(),
                        customerId,
                        r.getStatus().name(),
                        r.getStartAt(),
                        r.getEndAt(),
                        r.getTotalAmountEur()
                ))
                .toList();
    }
}