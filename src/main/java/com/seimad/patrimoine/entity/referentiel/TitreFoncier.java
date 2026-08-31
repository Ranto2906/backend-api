package com.seimad.patrimoine.entity.referentiel;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Titre foncier ou parcelle cadastrale.
 * La clé UUID est générée côté client mobile (pas par la base).
 */
@Entity
@Table(name = "titre_foncier", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"numero", "id_propriete"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TitreFoncier {

    @Id
    @Column(name = "id_titre_foncier")
    private UUID idTitreFoncier;

    @Column(name = "numero", nullable = false, length = 50)
    private String numero;

    @Column(name = "type_titre", length = 30)
    @Builder.Default
    private String typeTitre = "Titre Foncier";

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
    @JoinColumn(name = "id_propriete", nullable = false)
    private Propriete propriete;
}
