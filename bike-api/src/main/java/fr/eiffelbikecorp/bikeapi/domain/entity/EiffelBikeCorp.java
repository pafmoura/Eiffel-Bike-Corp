package fr.eiffelbikecorp.bikeapi.domain.entity;

import jakarta.persistence.Entity;
import lombok.ToString;

import java.util.UUID;

@ToString(callSuper = true)
@Entity
public class EiffelBikeCorp extends BikeProvider {
    public EiffelBikeCorp() {
        if (super.getId() == null) {
            super.setId(UUID.randomUUID());
        }
    }
}
