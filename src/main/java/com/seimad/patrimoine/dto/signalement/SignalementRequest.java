package com.seimad.patrimoine.dto.signalement;

import jakarta.validation.constraints.NotNull;
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
public class SignalementRequest {

    /** Référence optionnelle (générée automatiquement si absente : SIG-AAAA-NNNN). */
    private String reference;

    private String description;

    private LocalDateTime dateSignalement;

    @NotNull(message = "Le type de signalement est obligatoire")
    private Integer idTypeSignalement;

    private Integer idStatutSignalement;

    private Integer idVille;

    private UUID idTitreFoncier;

    private UUID idParcelle;

    private Integer idDossier;

    private UUID idNotification;

    private UUID idAvertissement;

    /** Optionnel : défaut = utilisateur connecté (ou null si appel non authentifié). */
    private Integer idUtilisateurCreation;
}