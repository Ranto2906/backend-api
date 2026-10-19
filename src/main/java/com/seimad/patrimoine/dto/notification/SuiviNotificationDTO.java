package com.seimad.patrimoine.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuiviNotificationDTO {
    private UUID idSuivi;
    private Integer ordre;
    private LocalDate dateSuivi;
    private String constats;
    private String actionsASuivre;
    private UUID idNotification;

    // ── Workflow (sur le modèle du suivi de dossier) ──
    private Integer idEtapeNotification;
    private String codeEtape;
    private String libelleEtape;
    private Integer ordreEtape;
    private Boolean etapeFinale;
    private Integer idStatutCouleur;
    private String codeStatutCouleur;
    private String libelleStatutCouleur;
    private String couleurStatutCouleurHex;
    private String commentaire;

    // Contexte de la notification (pour affichage dans les listes)
    private String numeroPropriete;
    private String numeroLot;
    private LocalDate dateNotification;
    private String statut;
}
