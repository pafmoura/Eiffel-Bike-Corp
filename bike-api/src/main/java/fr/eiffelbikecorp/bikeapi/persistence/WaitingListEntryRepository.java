package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.entity.WaitingListEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaitingListEntryRepository extends JpaRepository<WaitingListEntry, Long> {

    List<WaitingListEntry> findByCustomer_IdAndServedAtIsNull(UUID customerId);

    Optional<WaitingListEntry> findByWaitingList_IdAndCustomer_Id(Long waitingListId, UUID customerId);

    Optional<WaitingListEntry> findFirstByWaitingList_IdAndServedAtIsNullOrderByCreatedAtAsc(Long waitingListId);

    boolean existsByWaitingList_IdAndCustomer_IdAndServedAtIsNull(Long waitingListId, UUID customerId);
}
