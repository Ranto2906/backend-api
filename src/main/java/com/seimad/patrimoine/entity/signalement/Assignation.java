package com.seimad.patrimoine.entity.signalement;

import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.notification.Avertissement;
import com.seimad.patrimoine.entity.notification.NotificationOccupation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Assignation : lien entre un signalement et une notification d'occupation
 * OU un avertissement (exactement une des deux cibles, vérifié en base).
 * Un signalement peut être assigné à plusieurs notifications/avertissements.
 */
@Entity
@Table(name = "assignation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_assignation")
    private Long idAssignation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_signalement", nullable = false)
    private Signalement signalement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_notification")
    private NotificationOccupation notification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_avertissement")
    private Avertissement avertissement;

    @Column(name = "date_assignation")
    @Builder.Default
    private LocalDateTime dateAssignation = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur")
    private Utilisateur utilisateur;
}
