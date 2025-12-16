package fr.eiffelbikecorp.bikeapi.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class SaleNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_offer_id", nullable = false)
    private SaleOffer saleOffer;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Simple: name of corp staff who wrote it.
     * (Keeps model consistent without adding a new Staff entity.)
     */
    @Column(length = 255)
    private String createdBy;
}
