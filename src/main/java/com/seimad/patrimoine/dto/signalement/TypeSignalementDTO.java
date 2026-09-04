package com.seimad.patrimoine.dto.signalement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeSignalementDTO {
    private Integer idTypeSignalement;
    private String code;
    private String libelle;
    private String couleur;
}