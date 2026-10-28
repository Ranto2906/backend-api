package com.seimad.patrimoine.dto.descente;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un changement de statut de la descente terrain reconstitué du journal
 * d'audit : date de modification et valeur du statut, pour l'affichage
 * de l'historique dans l'application web.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DescenteStatutHistoriqueDTO {

    /** Champ suivi : {@code statutConstat} ou {@code validation}. */
    private String champ;

    /** Libellé français du champ : « Statut du constat » / « Validation ». */
    private String libelleChamp;

    /** Valeur précédente — {@code null} sur l'état initial (création). */
    private String ancienStatut;

    /** Nouvelle valeur du statut. */
    private String nouveauStatut;

    /** Date de modification (format ISO issu de l'audit). */
    private String dateModification;

    /** Action à l'origine de la ligne : {@code CREATE} ou {@code UPDATE}. */
    private String action;

    /** Utilisateur ayant effectué le changement (nullable). */
    private String nomUtilisateur;
}
