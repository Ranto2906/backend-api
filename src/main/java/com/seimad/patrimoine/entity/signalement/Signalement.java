package com.seimad.patrimoine.entity.signalement;

import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.dossier.Dossier;
import com.seimad.patrimoine.entity.notification.Avertissement;
import com.seimad.patrimoine.entity.notification.NotificationOccupation;
import com.seimad.patrimoine.entity.referentiel.Parcelle;
import com.seimad.patrimoine.entity.referentiel.TitreFoncier;
import com.seimad.patrimoine.entity.referentiel.Ville;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Signalement soumis depuis le mobile, géré côté web.
 * Les FK vers terrain/dossier sont nullable (rattachement possible après coup).
 * La clé UUID est générée côté client mobile.
 */
@Entity
@Table(name = "signalement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Signalement {

    @Id
    @Column(name = "id_signalement")
    private UUID idSignalement;

    @Column(name = "reference", unique = true, length = 30)
    private String reference;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_signalement")
    @Builder.Default
    private LocalDateTime dateSignalement = LocalDateTime.now();

    @Column(name = "date_modification")
    @Builder.Default
    private LocalDateTime dateModification = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_type_signalement", nullable = false)
    private TypeSignalement typeSignalement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_statut_signalement")
    private StatutSignalement statutSignalement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ville")
    private Ville ville;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_titre_foncier")
    private TitreFoncier titreFoncier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_parcelle")
    private Parcelle parcelle;

    @Column(name = "commentaire_traitement", columnDefinition = "TEXT")
    private String commentaireTraitement;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur_traitement")
    private Utilisateur utilisateurTraitement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dossier")
    private Dossier dossier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_notification")
    private NotificationOccupation notificationOccupation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_avertissement")
    private Avertissement avertissement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur_creation", nullable = false)
    private Utilisateur utilisateurCreation;
}
