package com.seimad.patrimoine.dto.referentiel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TitreFoncierRequest {
    private String idTitreFoncier;

    @NotBlank(message = "Le numéro est obligatoire")
    private String numero;

    private String typeTitre;
    private String zone;
    private String localisation;
    private BigDecimal superficieTotale;

    @NotNull(message = "La propriété est obligatoire")
    private Integer idPropriete;
}
