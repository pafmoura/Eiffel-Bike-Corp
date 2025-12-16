package fr.eiffelbikecorp.bikeapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Customer extends Provider {

    @Column(nullable = false, length = 255)
    private String fullName;

    @Column(nullable = false, length = 255, unique = true)
    private String email;
}
