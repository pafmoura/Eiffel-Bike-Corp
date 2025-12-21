package fr.eiffelbikecorp.bikeapi.domain.entity;

import fr.eiffelbikecorp.bikeapi.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "sale_payment")
public class SalePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal originalAmount;

    @Column(nullable = false, length = 10)
    private String originalCurrency;

    @Column(nullable = false, precision = 18, scale = 10)
    private BigDecimal fxRateToEur;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amountEur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    private LocalDateTime paidAt;

    @Column(length = 100)
    private String stripePaymentIntentId;
}
