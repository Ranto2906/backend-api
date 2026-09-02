package com.seimad.patrimoine.dto.referentiel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProprieteRequest {
    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotNull(message = "La ville est obligatoire")
    private Integer idVille;
}
