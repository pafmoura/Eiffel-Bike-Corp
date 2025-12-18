package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.EiffelBikeCorp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EiffelBikeCorpRepository extends JpaRepository<EiffelBikeCorp, UUID> {
}
