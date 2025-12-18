package fr.eiffelbikecorp.bikeapi.domain;

import jakarta.persistence.Entity;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@ToString(callSuper = true)
@Entity
public class Employee extends BikeProvider {
    public Employee() {
        if (super.getId() == null) {
            super.setId(UUID.randomUUID());
        }
    }
}
