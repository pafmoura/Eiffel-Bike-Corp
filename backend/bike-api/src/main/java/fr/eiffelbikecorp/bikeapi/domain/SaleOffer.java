package fr.eiffelbikecorp.bikeapi.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(name = "uk_sale_offer_bike", columnNames = "bike_id")
)
public class SaleOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_id", nullable = false)
    private Bike bike;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_corp_id", nullable = false)
    private EiffelBikeCorp seller;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal askingPriceEur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SaleOfferStatus status;

    @Column(nullable = false)
    private LocalDateTime listedAt;

    private LocalDateTime soldAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_customer_id")
    private Customer buyer;

    @OneToMany(mappedBy = "saleOffer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<SaleNote> notes = new ArrayList<>();
}
