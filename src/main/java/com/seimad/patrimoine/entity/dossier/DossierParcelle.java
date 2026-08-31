package com.seimad.patrimoine.entity.dossier;

import com.seimad.patrimoine.entity.referentiel.Parcelle;
import com.seimad.patrimoine.entity.referentiel.TitreFoncier;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ligne dossier-parcelle : point d'ancrage unique pour geometrie et descente_terrain.
 * La clé UUID est générée côté client mobile.
 */
@Entity
@Table(name = "dossier_parcelle", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_dossier", "id_titre_foncier", "id_parcelle"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DossierParcelle {

    @Id
    @Column(name = "id_dossier_parcelle")
    private UUID idDossierParcelle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dossier", nullable = false)
    private Dossier dossier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_titre_foncier", nullable = false)
    private TitreFoncier titreFoncier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_parcelle")
    private Parcelle parcelle;

    @Column(name = "superficie_m2", precision = 12, scale = 3)
    private BigDecimal superficieM2;

    @Column(name = "valeur_demande_ttc", precision = 16, scale = 2)
    private BigDecimal valeurDemandeTtc;

    @Column(name = "valeur_demande_ht", precision = 16, scale = 2)
    private BigDecimal valeurDemandeHt;

    @Column(name = "observation", columnDefinition = "TEXT")
    private String observation;

    @Column(name = "date_ajout")
    @Builder.Default
    private LocalDateTime dateAjout = LocalDateTime.now();

    @Column(name = "coordonnee_x_", precision = 12, scale = 3)
    private BigDecimal coordonneeX;

    @Column(name = "coordonnee_y_", precision = 12, scale = 3)
    private BigDecimal coordonneeY;
}
