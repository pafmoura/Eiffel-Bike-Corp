package fr.eiffelbikecorp.bikeapi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "purchase_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_purchase_offer",
                columnNames = {"purchase_id", "sale_offer_id"}
        )
)
public class PurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_offer_id", nullable = false)
    private SaleOffer offer;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceEurSnapshot;
}
