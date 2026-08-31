package com.seimad.patrimoine.entity.notification;

import com.seimad.patrimoine.entity.referentiel.Parcelle;
import com.seimad.patrimoine.entity.referentiel.TitreFoncier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "avertissement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Avertissement {

    @Id
    @Column(name = "id_avertissement")
    private UUID idAvertissement;

    @Column(name = "date_avertissement")
    private LocalDate dateAvertissement;

    @Column(name = "annee")
    private Integer annee;

    @Column(name = "informations_occupants", columnDefinition = "TEXT")
    private String informationsOccupants;

    @Column(name = "constats", columnDefinition = "TEXT")
    private String constats;

    @Column(name = "actions_entreprises", columnDefinition = "TEXT")
    private String actionsEntreprises;

    @Column(name = "a_faire", columnDefinition = "TEXT")
    private String aFaire;

    @Column(name = "mission", columnDefinition = "TEXT")
    private String mission;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_personne")
    private Personne personne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_parcelle")
    private Parcelle parcelle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_titre_foncier", nullable = false)
    private TitreFoncier titreFoncier;
}
