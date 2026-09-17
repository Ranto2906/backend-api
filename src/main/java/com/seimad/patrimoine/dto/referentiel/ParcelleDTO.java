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
public class ParcelleDTO {
    private String idParcelle;
    private String numeroLot;
    private BigDecimal superficieM2;
    private String observation;
    private Integer idPropriete;
    private String numeroPropriete;
    private String nomPropriete;
    private String zonePropriete;
    private String localisationPropriete;
    private String nomVille;
    private String createdAt;
    private String updatedAt;
}
