package com.seimad.patrimoine.dto.referentiel;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Occupation d'une propriété par les dossiers qui lui sont rattachés :
 * liste des dossiers (N° dossier, demandeur, superficie) et calcul de la
 * superficie restante — quand le reste vaut 0, plus aucune demande ne peut
 * être rattachée à la propriété.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProprieteOccupationDTO {
    private Integer idPropriete;
    /** Superficie totale enregistrée (null = non renseignée, pas de limite). */
    private BigDecimal superficieTotale;
    /** Somme des superficies des dossiers rattachés. */
    private BigDecimal superficieUtilisee;
    /** SuperficieTotale - SuperficieUtilisee (null si totale non renseignée). */
    private BigDecimal superficieRestante;
    /** false quand le reste vaut 0 (ou négatif) : plus aucune demande possible. */
    private boolean disponible;
    private List<DossierLieDTO> dossiers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DossierLieDTO {
        private Integer idDossier;
        private String numeroDossier;
        /** Nom du demandeur principal (ou du premier demandeur rattaché). */
        private String demandeur;
        /** Superficie occupée par ce dossier sur la propriété. */
        private BigDecimal superficieM2;
    }
}
