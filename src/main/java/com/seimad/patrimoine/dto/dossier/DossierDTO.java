package com.seimad.patrimoine.dto.dossier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dossier complet : informations de la demande, statut, ville,
 * demandeurs (personnes) et historique de suivi (étapes).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DossierDTO {

    private Integer idDossier;
    private String numeroDossier;
    private LocalDate dateDemande;
    private Integer annee;
    private String transmission;
    private String resultatEtude;
    private String observation;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private LocalDate dateValidation;
    private BigDecimal montantTotal;

    // ── Statut ──
    private Integer idStatutDossier;
    private String codeStatut;
    private String libelleStatut;
    private String couleurStatutHex;
    private Boolean statutFinal;

    // ── Référentiel du dossier ──
    private Integer idTypeDossier;
    private String libelleTypeDossier;
    private Integer idDossierFournie;
    private String libelleDossierFournie;

    // ── Ville ──
    private Integer idVille;
    private String nomVille;

    // ── Demandeurs (personnes) ──
    private List<DemandeurDTO> demandeurs;

    // ── Parcelles demandées ──
    private List<ParcelleDemandeeDTO> parcelles;

    // ── Suivi (étapes) ──
    private List<SuiviDossierDTO> suivi;

    // ── Résumé de l'avancement ──
    private Integer etapeActuelleOrdre;
    private String etapeActuelleLibelle;
    private Integer nombreEtapesRealisees;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DemandeurDTO {
        private Integer idPersonne;
        private String nom;
        private String contact;
        private String email;
        private String adresse;
        private String role;          // rôle dans dossier_demandeur (Principal…)
        private String codeTypePersonne;
        private String libelleTypePersonne;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParcelleDemandeeDTO {
        private java.util.UUID idDossierParcelle;
        private Integer idPropriete;
        private String nomPropriete;
        private String numeroPropriete;
        private java.util.UUID idParcelle;
        private String numeroLot;
        private BigDecimal superficieM2;
        private String observation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuiviDossierDTO {
        private Integer idSuivi;
        private Integer idEtape;
        private String codeEtape;
        private String libelleEtape;
        private Integer ordreEtape;
        private Integer dureePrevisionnelle;
        private LocalDate dateRealisation;
        private Integer idStatutCouleur;
        private String codeStatutCouleur;
        private String libelleStatutCouleur;
        private String couleurStatutCouleurHex;
        private String commentaire;
        private String nomUtilisateur;
        private LocalDateTime dateCreation;
        private LocalDateTime dateModification;
    }
}
