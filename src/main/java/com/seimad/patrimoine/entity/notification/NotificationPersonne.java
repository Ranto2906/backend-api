package com.seimad.patrimoine.entity.notification;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "notification_personne")
@IdClass(NotificationPersonneId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPersonne {

    @Id
    @Column(name = "id_notification")
    private UUID idNotification;

    @Id
    @Column(name = "id_personne")
    private Integer idPersonne;

    @Column(name = "role_dans_notification", length = 50)
    @Builder.Default
    private String roleDansNotification = "Notifié";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_notification", insertable = false, updatable = false)
    private NotificationOccupation notificationOccupation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_personne", insertable = false, updatable = false)
    private Personne personne;
}
