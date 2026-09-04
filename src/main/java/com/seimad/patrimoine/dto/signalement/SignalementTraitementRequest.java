package com.seimad.patrimoine.dto.signalement;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalementTraitementRequest {

    @NotNull(message = "Le statut est obligatoire")
    private Integer idStatutSignalement;

    private String commentaireTraitement;

    /** Optionnel : défaut = utilisateur connecté. */
    private Integer idUtilisateurTraitement;
}