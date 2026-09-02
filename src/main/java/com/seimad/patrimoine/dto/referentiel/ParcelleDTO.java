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
    private String idTitreFoncier;
    private String numeroTitreFoncier;
    private String createdAt;
    private String updatedAt;
}
