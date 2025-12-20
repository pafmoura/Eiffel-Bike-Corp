package fr.eiffelbikecorp.bikeapi.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "basket_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_basket_offer",
                columnNames = {"basket_id", "sale_offer_id"}
        )
)
public class BasketItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "basket_id", nullable = false)
    private Basket basket;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_offer_id", nullable = false)
    private SaleOffer offer;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceEurSnapshot;

    @Column(nullable = false)
    private LocalDateTime addedAt;
}
