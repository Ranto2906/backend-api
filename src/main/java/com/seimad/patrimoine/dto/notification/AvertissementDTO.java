package com.seimad.patrimoine.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvertissementDTO {
    private UUID idAvertissement;
    private LocalDate dateAvertissement;
    private Integer annee;
    private String informationsOccupants;
    private String constats;
    private String actionsEntreprises;
    private String aFaire;
    private String mission;
    private LocalDateTime createdAt;

    // Relations
    private UUID idParcelle;
    private String numeroLot;
    private UUID idTitreFoncier;
    private String numeroTitre;

    // Personne associée
    private Integer idPersonne;
    private String nomPersonne;
}
