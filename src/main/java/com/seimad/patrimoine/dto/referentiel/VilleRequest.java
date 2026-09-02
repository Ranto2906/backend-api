package com.seimad.patrimoine.dto.referentiel;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VilleRequest {
    @NotBlank(message = "Le nom de la ville est obligatoire")
    private String nomVille;
}
