package com.seimad.patrimoine.entity.notification;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Référentiel des étapes du workflow de suivi de notification,
 * sur le modèle du référentiel « etape » des dossiers.
 */
@Entity
@Table(name = "etape_notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtapeNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_etape_notification")
    private Integer idEtapeNotification;

    @Column(name = "code_etape", nullable = false, unique = true, length = 50)
    private String codeEtape;

    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;

    @Column(name = "ordre", nullable = false, unique = true)
    private Integer ordre;

    @Column(name = "est_final", nullable = false)
    @Builder.Default
    private Boolean estFinal = false;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
