package fr.eiffelbikecorp.bikeapi.domain.entity;

import fr.eiffelbikecorp.bikeapi.domain.enums.SaleOfferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "sale_offer",
        uniqueConstraints = @UniqueConstraint(name = "uk_sale_offer_bike", columnNames = "bike_id"))
public class SaleOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "bike_id", nullable = false)
    private Bike bike;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal askingPriceEur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SaleOfferStatus status;

    @Column(nullable = false)
    private LocalDateTime listedAt;

    private LocalDateTime soldAt;

    @ManyToOne
    @JoinColumn(name = "buyer_customer_id")
    private Customer buyer;

    @Builder.Default
    @OneToMany(mappedBy = "saleOffer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<SaleNote> notes = new ArrayList<>();

}
