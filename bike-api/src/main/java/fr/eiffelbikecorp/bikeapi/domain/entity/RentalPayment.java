package fr.eiffelbikecorp.bikeapi.domain.entity;

import fr.eiffelbikecorp.bikeapi.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class RentalPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal originalAmount;

    @Column(nullable = false, length = 3)
    private String originalCurrency; // e.g. "USD", "BRL"

    /**
     * Conversion snapshot (so history remains correct).
     */
    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal fxRateToEur;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amountEur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @Column(nullable = false)
    private LocalDateTime paidAt;

    @Column(length = 128)
    private String stripePaymentIntentId;
}
