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
public class LieuRequest {

    @NotBlank(message = "Le libellé du lieu est obligatoire")
    private String libelle;

    @NotNull(message = "La ville du lieu est obligatoire")
    private Integer idVille;
}
