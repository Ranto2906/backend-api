package com.seimad.patrimoine.dto.referentiel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeProprieteDTO {
    private Integer idTypePropriete;
    private String libelle;
}
