package com.seimad.patrimoine.dto.referentiel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProprieteDTO {
    private Integer idPropriete;
    private String nom;
    private Integer idVille;
    private String nomVille;
    private String createdAt;
    private String updatedAt;
}
