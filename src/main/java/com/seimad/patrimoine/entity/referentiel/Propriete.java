package com.seimad.patrimoine.entity.referentiel;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "propriete")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Propriete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_propriete")
    private Integer idPropriete;

    @Column(name = "nom", nullable = false, unique = true, length = 200)
    private String nom;

    @Column(name = "numero", length = 50)
    private String numero;

    @Column(name = "zone", length = 150)
    private String zone;

    @Column(name = "localisation", columnDefinition = "TEXT")
    private String localisation;

    @Column(name = "superficie_totale", precision = 12, scale = 3)
    private BigDecimal superficieTotale;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ville")
    private Ville ville;
}
