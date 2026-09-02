package com.seimad.patrimoine.dto.referentiel;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelleRequest {
    private String idParcelle;
    private String numeroLot;
    private BigDecimal superficieM2;
    private String observation;

    @NotBlank(message = "Le titre foncier est obligatoire")
    private String idTitreFoncier;
}
