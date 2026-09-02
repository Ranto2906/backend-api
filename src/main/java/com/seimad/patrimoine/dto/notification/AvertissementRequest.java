package com.seimad.patrimoine.dto.notification;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvertissementRequest {
    private LocalDate dateAvertissement;
    private Integer annee;
    private String informationsOccupants;
    private String constats;
    private String actionsEntreprises;
    private String aFaire;
    private String mission;

    @NotNull(message = "La personne est obligatoire")
    private Integer idPersonne;

    @NotNull(message = "La parcelle est obligatoire")
    private UUID idParcelle;

    @NotNull(message = "Le titre foncier est obligatoire")
    private UUID idTitreFoncier;
}
