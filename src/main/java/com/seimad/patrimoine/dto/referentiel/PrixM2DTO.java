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
public class PrixM2DTO {
    private Integer idPrix;
    private BigDecimal prixTtc;
    private BigDecimal prixHt;
    private String observation;
    private String dateMaj;
    private Integer idVille;
    private String nomVille;
    private String createdAt;
}
