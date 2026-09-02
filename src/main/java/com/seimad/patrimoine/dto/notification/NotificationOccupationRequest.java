package com.seimad.patrimoine.dto.notification;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationOccupationRequest {

    private LocalDate dateNotification;
    private Integer annee;
    private LocalDateTime dateConvocation;
    private String informationsOccupants;
    private String constats;
    private String doleances;
    private String actionsEntreprises;

    @NotNull(message = "Le statut est obligatoire")
    private String statut;

    @NotNull(message = "La parcelle est obligatoire")
    private UUID idParcelle;

    @NotNull(message = "Le titre foncier est obligatoire")
    private UUID idTitreFoncier;

    // Personnes associées (optionnel)
    private List<NotificationPersonneRequest> personnes;
}
