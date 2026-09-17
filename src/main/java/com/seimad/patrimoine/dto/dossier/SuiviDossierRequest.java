package com.seimad.patrimoine.dto.dossier;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Requête de suivi : enregistrement (ou mise à jour) d'une étape réalisée
 * sur un dossier — le « changement d'étape » du suivi de dossier.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuiviDossierRequest {

    @NotNull(message = "L'étape est obligatoire")
    private Integer idEtape;

    private LocalDate dateRealisation;

    /** Couleur d'avancement de l'étape (Vert / Jaune / Rouge…) — id de statut_couleur. */
    private Integer idStatutCouleur;

    private String commentaire;
}
