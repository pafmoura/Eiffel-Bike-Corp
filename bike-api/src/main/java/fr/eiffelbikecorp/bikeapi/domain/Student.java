package fr.eiffelbikecorp.bikeapi.domain;

import jakarta.persistence.Entity;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@ToString(callSuper = true)
@Entity
public class Student extends BikeProvider {
    public Student() {
        if (super.getId() == null) {
            super.setId(UUID.randomUUID());
        }
    }
}
