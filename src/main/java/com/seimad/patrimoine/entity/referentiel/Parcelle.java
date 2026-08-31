package com.seimad.patrimoine.entity.referentiel;

import com.seimad.patrimoine.entity.auth.Utilisateur;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Parcelle rattachée à un titre foncier.
 * La clé UUID est générée côté client mobile (pas par la base).
 */
@Entity
@Table(name = "parcelle", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"numero_lot", "id_titre_foncier"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parcelle {

    @Id
    @Column(name = "id_parcelle")
    private UUID idParcelle;

    @Column(name = "numero_lot", length = 50)
    private String numeroLot;

    @Column(name = "superficie_m2", precision = 12, scale = 3)
    private BigDecimal superficieM2;

    @Column(name = "observation", columnDefinition = "TEXT")
    private String observation;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur_creation")
    private Utilisateur utilisateurCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_titre_foncier", nullable = false)
    private TitreFoncier titreFoncier;
}
