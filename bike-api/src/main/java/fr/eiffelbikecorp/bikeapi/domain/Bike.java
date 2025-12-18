package fr.eiffelbikecorp.bikeapi.domain;

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

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "offered_by_provider_id", nullable = false)
    private BikeProvider offeredBy;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal rentalDailyRateEur;

    @OneToOne(mappedBy = "bike", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private WaitingList waitingList;

    @OneToOne(mappedBy = "bike", fetch = FetchType.LAZY)
    private SaleOffer saleOffer;
}
