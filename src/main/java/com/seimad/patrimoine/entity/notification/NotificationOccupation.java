package com.seimad.patrimoine.entity.notification;

import com.seimad.patrimoine.entity.referentiel.Parcelle;
import com.seimad.patrimoine.entity.referentiel.TitreFoncier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_occupation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationOccupation {

    @Id
    @Column(name = "id_notification")
    private UUID idNotification;

    @Column(name = "date_notification")
    private LocalDate dateNotification;

    @Column(name = "annee")
    private Integer annee;

    @Column(name = "date_convocation")
    private LocalDateTime dateConvocation;

    @Column(name = "informations_occupants", columnDefinition = "TEXT")
    private String informationsOccupants;

    @Column(name = "constats", columnDefinition = "TEXT")
    private String constats;

    @Column(name = "doleances", columnDefinition = "TEXT")
    private String doleances;

    @Column(name = "actions_entreprises", columnDefinition = "TEXT")
    private String actionsEntreprises;

    @Column(name = "statut", length = 30)
    @Builder.Default
    private String statut = "En cours";

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_parcelle")
    private Parcelle parcelle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_titre_foncier", nullable = false)
    private TitreFoncier titreFoncier;
}
