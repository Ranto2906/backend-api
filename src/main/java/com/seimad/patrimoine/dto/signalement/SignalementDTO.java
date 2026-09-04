package com.seimad.patrimoine.dto.signalement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalementDTO {
    private UUID idSignalement;
    private String reference;
    private String description;
    private LocalDateTime dateSignalement;
    private LocalDateTime dateModification;

    // Type
    private Integer idTypeSignalement;
    private String codeType;
    private String libelleType;
    private String couleurType;

    // Statut
    private Integer idStatutSignalement;
    private String codeStatut;
    private String libelleStatut;
    private String couleurStatutHex;
    private Boolean statutFinal;

    // Localisation
    private Integer idVille;
    private String nomVille;
    private UUID idTitreFoncier;
    private String numeroTitre;
    private UUID idParcelle;
    private String numeroLot;

    // Traitement
    private String commentaireTraitement;
    private LocalDateTime dateTraitement;
    private Integer idUtilisateurTraitement;
    private String nomUtilisateurTraitement;

    // Rattachés possibles
    private Integer idDossier;
    private String numeroDossier;
    private UUID idNotification;
    private UUID idAvertissement;

    // Créateur
    private Integer idUtilisateurCreation;
    private String nomUtilisateurCreation;
}