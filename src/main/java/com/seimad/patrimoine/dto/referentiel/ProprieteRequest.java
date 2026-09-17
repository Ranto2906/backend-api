package com.seimad.patrimoine.dto.referentiel;

import com.seimad.patrimoine.dto.carte.GeometrieRequest;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProprieteRequest {
    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le numéro est obligatoire")
    private String numero;

    private String zone;
    private String localisation;
    private BigDecimal superficieTotale;

    @NotNull(message = "La ville est obligatoire")
    private Integer idVille;

    /** Géométrie de la propriété (optionnelle) — point d'ancrage dans la table `geometrie`. */
    private GeometrieRequest geometrie;
}
