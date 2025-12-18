package fr.eiffelbikecorp.bikeapi.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@ToString(callSuper = true)
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Customer {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 255)
    private String fullName;

    @Column(nullable = false, length = 255)
    private String email;

    public Customer() {
        if (this.getId() == null) {
            this.setId(UUID.randomUUID());
        }
    }
}
