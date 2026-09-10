package com.seimad.patrimoine.dto.signalement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Assignation d'un signalement à une notification d'occupation ou à un
 * avertissement. typeCible vaut "notification" ou "avertissement" ; seul le
 * bloc correspondant est renseigné (null sinon, JSON non_null).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignationDTO {

    private Long idAssignation;

    // Signalement
    private UUID idSignalement;
    private String referenceSignalement;

    // Cible
    private String typeCible;

    // Si notification
    private UUID idNotification;
    private LocalDate dateNotification;
    private String numeroProprieteNotification;

    // Si avertissement
    private UUID idAvertissement;
    private LocalDate dateAvertissement;
    private String numeroProprieteAvertissement;

    // Trace
    private LocalDateTime dateAssignation;
    private Integer idUtilisateur;
    private String nomUtilisateur;
}
