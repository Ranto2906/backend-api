package com.seimad.patrimoine.entity.descente;

import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.dossier.Demandeur;
import com.seimad.patrimoine.entity.dossier.DossierParcelle;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Descente terrain : vérification avant-vente, indépendante des étapes du dossier.
 * Créée hors-ligne sur le mobile, comme le signalement.
 * La clé UUID est générée côté client mobile.
 */
@Entity
@Table(name = "descente_terrain")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DescenteTerrain {

    @Id
    @Column(name = "id_descente")
    private UUID idDescente;

    @Column(name = "reference", unique = true, length = 30)
    private String reference;

    @Column(name = "date_descente")
    private LocalDate dateDescente;

    @Column(name = "statut_constat", length = 50)
    @Builder.Default
    private String statutConstat = "En attente";

    @Column(name = "observation", columnDefinition = "TEXT")
    private String observation;

    @Column(name = "mode", length = 20)
    @Builder.Default
    private String mode = "online";

    @Column(name = "validation", length = 30)
    @Builder.Default
    private String validation = "En attente";

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "synchronise")
    @Builder.Default
    private Boolean synchronise = false;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "date_modification")
    @Builder.Default
    private LocalDateTime dateModification = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur_creation", nullable = false)
    private Utilisateur utilisateurCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur_validation")
    private Utilisateur utilisateurValidation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_demandeur", nullable = false)
    private Demandeur demandeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dossier_parcelle", nullable = false)
    private DossierParcelle dossierParcelle;
}
