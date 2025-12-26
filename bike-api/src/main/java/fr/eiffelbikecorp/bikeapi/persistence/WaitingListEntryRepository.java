package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.entity.Rental;
import fr.eiffelbikecorp.bikeapi.domain.entity.WaitingListEntry;
import fr.eiffelbikecorp.bikeapi.domain.enums.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaitingListEntryRepository extends JpaRepository<WaitingListEntry, Long> {

    // FIFO behavior (oldest first)
    List<WaitingListEntry> findByWaitingList_IdOrderByCreatedAtAsc(Long waitingListId);
    Optional<WaitingListEntry> findFirstByWaitingList_IdOrderByCreatedAtAsc(Long waitingListId);

    // prevent duplicates: customer can’t join same bike waiting list twice
    boolean existsByWaitingList_IdAndCustomer_Id(Long waitingListId, UUID customerId);

    long countByWaitingList_Id(Long waitingListId);

    List<WaitingListEntry> findByCustomer_IdAndServedAtIsNull(UUID customerId);

    Optional<WaitingListEntry> findByWaitingList_IdAndCustomer_Id(Long waitingListId, UUID customerId);

    Optional<WaitingListEntry> findFirstByWaitingList_IdAndServedAtIsNullOrderByCreatedAtAsc(Long waitingListId);

    boolean existsByWaitingList_IdAndCustomer_IdAndServedAtIsNull(Long waitingListId, UUID customerId);


}
