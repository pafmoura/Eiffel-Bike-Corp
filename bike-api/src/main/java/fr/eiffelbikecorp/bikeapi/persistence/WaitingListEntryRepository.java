package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.WaitingListEntry;
import org.springframework.data.jpa.repository.JpaRepository;

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

    // convenience: all entries for a customer (e.g., “my waiting lists”)
    List<WaitingListEntry> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);
}
