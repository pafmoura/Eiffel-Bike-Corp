package fr.eiffelbikecorp.bikeapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Entity
public class EiffelBikeCorp extends Provider {

    @Column(nullable = false, length = 255)
    private String name;
}
