package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.WaitingList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WaitingListRepository extends JpaRepository<WaitingList, Long> {

    // US_09 put a bike in waiting list if not available
    Optional<WaitingList> findByBike_Id(Long bikeId);
    boolean existsByBike_Id(Long bikeId);
}
