package com.seimad.patrimoine.dto.referentiel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeProprieteRequest {

    @NotBlank(message = "Le libellé du type de propriété est obligatoire")
    @Size(max = 80, message = "Le libellé ne peut pas dépasser 80 caractères")
    private String libelle;
}
