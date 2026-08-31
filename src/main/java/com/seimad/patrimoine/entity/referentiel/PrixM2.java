package com.seimad.patrimoine.entity.referentiel;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "prix_m2")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrixM2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prix")
    private Integer idPrix;

    @Column(name = "prix_ttc", nullable = false, precision = 14, scale = 2)
    private BigDecimal prixTtc;

    @Column(name = "prix_ht", precision = 14, scale = 2)
    private BigDecimal prixHt;

    @Column(name = "observation", length = 255)
    private String observation;

    @Column(name = "date_maj", nullable = false)
    @Builder.Default
    private LocalDate dateMaj = LocalDate.now();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ville", nullable = false)
    private Ville ville;
}
