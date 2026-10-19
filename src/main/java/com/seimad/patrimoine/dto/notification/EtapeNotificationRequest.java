package com.seimad.patrimoine.dto.notification;

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
public class EtapeNotificationRequest {

    @NotBlank(message = "Le code de l'étape est obligatoire")
    private String codeEtape;

    @NotBlank(message = "Le libellé de l'étape est obligatoire")
    private String libelle;

    @NotNull(message = "L'ordre de l'étape est obligatoire")
    private Integer ordre;

    private Boolean estFinal;
}
