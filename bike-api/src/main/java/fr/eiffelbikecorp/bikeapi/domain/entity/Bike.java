package fr.eiffelbikecorp.bikeapi.domain.entity;

import fr.eiffelbikecorp.bikeapi.domain.enums.BikeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class Bike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BikeStatus status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "offered_by_provider_id", nullable = false)
    private BikeProvider offeredBy;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal rentalDailyRateEur;

    @OneToOne(mappedBy = "bike", cascade = CascadeType.ALL, orphanRemoval = true)
    private WaitingList waitingList;

    @OneToOne(mappedBy = "bike")
    private SaleOffer saleOffer;

}
