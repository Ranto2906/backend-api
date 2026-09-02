package com.seimad.patrimoine.dto.referentiel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TitreFoncierDTO {
    private String idTitreFoncier;
    private String numero;
    private String typeTitre;
    private String zone;
    private String localisation;
    private BigDecimal superficieTotale;
    private Integer idPropriete;
    private String nomPropriete;
    private String createdAt;
    private String updatedAt;
}
