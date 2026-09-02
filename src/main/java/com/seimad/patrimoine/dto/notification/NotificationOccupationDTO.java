package com.seimad.patrimoine.dto.notification;

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
public class NotificationOccupationDTO {
    private UUID idNotification;
    private LocalDate dateNotification;
    private Integer annee;
    private LocalDateTime dateConvocation;
    private String informationsOccupants;
    private String constats;
    private String doleances;
    private String actionsEntreprises;
    private String statut;
    private LocalDateTime createdAt;

    // Relations
    private UUID idParcelle;
    private String numeroLot;
    private UUID idTitreFoncier;
    private String numeroTitre;

    // Personnes associées
    private List<NotificationPersonneDTO> personnes;

    // Suivis
    private List<SuiviNotificationDTO> suivis;
}
