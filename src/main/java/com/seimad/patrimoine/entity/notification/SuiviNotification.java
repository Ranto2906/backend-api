package com.seimad.patrimoine.entity.notification;

import com.seimad.patrimoine.entity.dossier.StatutCouleur;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "suivi_notification", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_notification", "ordre"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuiviNotification {

    @Id
    @Column(name = "id_suivi")
    private UUID idSuivi;

    @Column(name = "ordre", nullable = false)
    private Integer ordre;

    @Column(name = "date_suivi")
    private LocalDate dateSuivi;

    @Column(name = "constats", columnDefinition = "TEXT")
    private String constats;

    @Column(name = "actions_a_suivre", columnDefinition = "TEXT")
    private String actionsASuivre;

    // ── Workflow (sur le modèle du suivi de dossier) ──

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_etape_notification")
    private EtapeNotification etapeNotification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_statut_couleur")
    private StatutCouleur statutCouleur;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_notification", nullable = false)
    private NotificationOccupation notificationOccupation;
}
