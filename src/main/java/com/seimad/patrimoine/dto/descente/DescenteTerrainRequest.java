package com.seimad.patrimoine.dto.descente;

import com.seimad.patrimoine.dto.carte.GeometrieRequest;
import jakarta.validation.constraints.NotNull;
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
public class DescenteTerrainRequest {

    /** Référence optionnelle (auto-générée DRT-AAAA-NNNN si absente). */
    private String reference;

    @NotNull(message = "La date de descente est obligatoire")
    private LocalDate dateDescente;

    @NotNull(message = "Le statut du constat est obligatoire")
    private String statutConstat;

    private String observation;

    /** 'online' ou 'offline'. */
    private String mode;

    private String validation;

    // ── Liaison vers le dossier existant (mode online) ──

    /** ID de l'utilisateur qui crée la descente (optionnel, résolu via JWT si absent). */
    private Integer idUtilisateurCreation;

    /** ID de la personne (demandeur) liée au dossier. */
    private Integer idPersonne;

    /** ID de la ligne dossier_parcelle. */
    private UUID idDossierParcelle;

    // ── Géométrie GPS (optionnelle) ──

    private GeometrieRequest geometrie;
}
