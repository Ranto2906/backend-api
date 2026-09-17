package com.seimad.patrimoine.dto.dossier;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Requête de création / modification d'une étape du workflow
 * (référentiel « Configuration des étapes » du suivi de dossier).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtapeRequest {

    /** Optionnel : généré automatiquement depuis le libellé si absent. */
    @Size(max = 50, message = "Le code de l'étape ne peut dépasser 50 caractères")
    private String codeEtape;

    @NotBlank(message = "Le libellé de l'étape est obligatoire")
    @Size(max = 150, message = "Le libellé de l'étape ne peut dépasser 150 caractères")
    private String libelle;

    @NotNull(message = "L'ordre de l'étape est obligatoire")
    private Integer ordre;

    /** Durée prévisionnelle en jours (optionnelle). */
    private Integer dureePrevisionnelle;
}
