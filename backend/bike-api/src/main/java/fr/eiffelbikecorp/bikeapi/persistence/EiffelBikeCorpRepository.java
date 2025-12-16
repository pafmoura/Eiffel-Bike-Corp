package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.EiffelBikeCorp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EiffelBikeCorpRepository extends JpaRepository<EiffelBikeCorp, Long> {
}
