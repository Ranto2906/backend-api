package com.seimad.patrimoine.dto.dossier;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Requête de création / modification d'un dossier (demande).
 * Le demandeur peut être :
 *  - une personne existante (idPersonne), et/ou
 *  - une nouvelle personne créée à la volée (demandeur.nom rempli).
 * Plusieurs demandeurs peuvent être transmis (liste).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DossierRequest {

    @NotBlank(message = "Le numéro de dossier est obligatoire")
    private String numeroDossier;

    private LocalDate dateDemande;

    private Integer annee;

    private String transmission;

    private String resultatEtude;

    private String observation;

    private LocalDate dateValidation;

    @DecimalMin(value = "0", message = "Le montant total doit être positif")
    private BigDecimal montantTotal;

    private Integer idStatutDossier;

    private Integer idTypeDossier;

    private String dossierFournie;

    @NotNull(message = "La ville est obligatoire")
    private Integer idVille;

    /** Demandeurs rattachés au dossier (personnes existantes ou à créer). */
    private List<DemandeurRequest> demandeurs;

    /**
     * Parcelles demandées (propriété / parcelle / superficie).
     * Si présent, le montant total du dossier est recalculé automatiquement
     * (somme des TTC) à partir du prix au m² de la ville.
     */
    private List<ParcelleDemandeeRequest> parcelles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DemandeurRequest {
        /** Personne existante (si connu). */
        private Integer idPersonne;
        /** Création inline : nom obligatoire si idPersonne absent. */
        private String nom;
        private String contact;
        private String email;
        private String adresse;
        /** Rôle dans le dossier (défaut : Principal). */
        private String role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParcelleDemandeeRequest {
        /** Propriété (titre foncier) — obligatoire. */
        @NotNull(message = "La propriété est obligatoire pour chaque parcelle demandée")
        private Integer idPropriete;
        /** Parcelle précise du titre (optionnel : absent = propriété entière). */
        private java.util.UUID idParcelle;
        /** Superficie demandée en m² (défaut : parcelle, sinon propriété). */
        @DecimalMin(value = "0", message = "La superficie doit être positive")
        private BigDecimal superficieM2;
        private String observation;
    }
}
