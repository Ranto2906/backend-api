package com.seimad.patrimoine.dto.referentiel;

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
public class PrixM2Request {
    @NotNull(message = "Le prix TTC est obligatoire")
    private BigDecimal prixTtc;

    private BigDecimal prixHt;
    private String observation;
    private String dateMaj;

    @NotNull(message = "La ville est obligatoire")
    private Integer idVille;
}
