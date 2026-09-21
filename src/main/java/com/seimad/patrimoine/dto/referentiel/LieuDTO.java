package com.seimad.patrimoine.dto.referentiel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LieuDTO {

    private Integer idLieu;
    private String libelle;
    private Integer idVille;
    private String nomVille;
}