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
public class ReturnNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false, unique = true)
    private Rental rental;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "author_customer_id", nullable = false)
    private Customer author;

    @Column(nullable = false, length = 2000)
    private String comment;

    @Column(length = 255)
    private String condition;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
