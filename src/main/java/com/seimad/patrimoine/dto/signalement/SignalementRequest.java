package com.seimad.patrimoine.dto.signalement;

import com.seimad.patrimoine.dto.carte.GeometrieRequest;
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

    /** Géométrie GPS du constat (optionnelle) — stockée dans la table `geometrie`.
     *  Reçue du mobile en GeoJSON (ex. {"type":"Point","coordinates":[lng,lat]}). */
    private GeometrieRequest geometrie;

    private Integer idPropriete;

    private UUID idParcelle;

    private Integer idDossier;

    /**
     * Optionnel : défaut = utilisateur connecté (ou null si appel non authentifié).
     * NB : le rattachement à une notification / un avertissement se fait désormais
     * via l'assignation (POST /signalements/{id}/assignations/...).
     */
    private Integer idUtilisateurCreation;
}