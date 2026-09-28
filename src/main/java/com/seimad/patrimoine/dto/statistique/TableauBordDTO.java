package com.seimad.patrimoine.dto.statistique;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tableau de bord — synthèse complète du patrimoine foncier :
 * <ul>
 *   <li>{@link Kpi} : compteurs principaux (dossiers, patrimoine, suivi,
 *       signalements, utilisateurs) et montant total ;</li>
 *   <li>{@link Avancement} : étapes / suivis des dossiers ;</li>
 *   <li>{@link GeometrieStats} : couverture cartographique (propriétés
 *       localisées sur la carte) ;</li>
 *   <li>{@link Finances} : montants par ville et par année ;</li>
 *   <li>séries mensuelles sur 12 mois (dossiers, notifications,
 *       signalements) ;</li>
 *   <li>répartitions (statuts, types, villes, géométries) avec pourcentages
 *       déjà calculés ;</li>
 *   <li>alertes, derniers dossiers / signalements, activité récente (audit).</li>
 * </ul>
 * Tous les champs optionnels sont omis en JSON (inclusion {@code non_null}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableauBordDTO {

    /** Date et heure de génération de la synthèse. */
    private LocalDateTime genereLe;

    private Kpi kpi;
    private Avancement avancement;
    private GeometrieStats geometrie;
    private Finances finances;

    /** 12 derniers mois (« YYYY-MM »), mois courant compris. */
    private List<SerieMensuelle> dossiersParMois;
    private List<SerieMensuelle> notificationsParMois;
    private List<SerieMensuelle> signalementsParMois;

    private List<Repartition> dossiersParStatut;
    private List<Repartition> dossiersParType;
    private List<Repartition> signalementsParStatut;
    private List<Repartition> signalementsParType;
    private List<Repartition> notificationsParStatut;
    private List<Repartition> descentesParValidation;
    private List<Repartition> descentesParConstat;
    private List<Repartition> proprietesParVille;
    private List<Repartition> geometriesParType;

    private List<AlerteTableauBord> alertes;
    private List<DossierRecent> derniersDossiers;
    private List<SignalementRecent> derniersSignalements;
    private List<ActiviteRecente> activiteRecente;

    // ─────────────────────────────────────────────────────────────
    // Sections
    // ─────────────────────────────────────────────────────────────

    /** Compteurs principaux du patrimoine et du suivi. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Kpi {
        // Dossiers
        private long nbDossiers;
        private long nbDossiersEnCours;
        private long nbDossiersClotures;
        private long nbDossiersAnneeCourante;
        private BigDecimal montantTotal;

        // Patrimoine
        private long nbProprietes;
        private BigDecimal superficieProprietes;
        private long nbParcelles;
        private BigDecimal superficieParcelles;
        private long nbVilles;
        private long nbLieux;

        // Suivi terrain
        private long nbNotifications;
        private long nbNotificationsEnCours;
        private long nbAvertissements;
        private long nbDescentes;
        private long nbDescentesEnAttente;

        // Signalements
        private long nbSignalements;
        private long nbSignalementsOuverts;

        // Acteurs & contenu
        private long nbPersonnes;
        private long nbDemandeurs;
        private long nbUtilisateurs;
        private long nbUtilisateursActifs;
        private long nbDocuments;
        private long nbAlertesNonLues;
    }

    /** Avancement du suivi des dossiers (étapes / suivis réalisés). */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Avancement {
        private long nbEtapes;
        private long nbSuivis;
        private long nbSuivisRealises;
        private long nbDossiersSuivis;
        /** % de suivis réalisés (0 s'il n'y a aucun suivi). */
        private double tauxRealisation;
        /** % de la grille dossiers × étapes renseignée. */
        private double tauxRemplissage;
    }

    /** Couverture cartographique des propriétés. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeometrieStats {
        private long nbGeometries;
        private BigDecimal superficieTotale;
        private long nbProprietesLocalisees;
        private long nbProprietesSansGeometrie;
        /** % de propriétés ayant une géométrie (0 si aucune propriété). */
        private double couverturePourcent;
    }

    /**
     * Synthèse financière : montants des demandes + CA réel / théorique
     * issus des vues SQL {@code vue_ca_reel} et {@code vue_ca_theorique}.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Finances {
        /** Montant total des dossiers de la période (équivalent au CA théorique). */
        private BigDecimal montantTotal;
        /** CA réel : dossiers au statut terminé uniquement. */
        private BigDecimal caReel;
        /** CA théorique : tous les dossiers, tous statuts confondus. */
        private BigDecimal caTheorique;
        /** Écart = CA théorique − CA réel (montants encore en cours de traitement). */
        private BigDecimal ecart;
        /** % de CA réel dans le CA théorique (1 décimale ; 0 si le théorique est nul). */
        private double tauxRealisation;
        private List<RepartitionMontant> parVille;
        private List<RepartitionMontant> parAnnee;
    }

    /** Valeur d'un mois (période « YYYY-MM »). */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SerieMensuelle {
        private String periode;
        private long valeur;
    }

    /** Catégorie d'une répartition (libellé, couleur hex, valeur, % du total). */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Repartition {
        private String libelle;
        private String couleur;
        private long valeur;
        private double pourcent;
    }

    /** Ligne financière (montant + nombre d'éléments). */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepartitionMontant {
        private String libelle;
        private BigDecimal montant;
        private long nombre;
    }

    /** Alerte calculée (niveau : info | warning | danger). */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlerteTableauBord {
        private String niveau;
        private String message;
    }

    /** Dernier dossier créé. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DossierRecent {
        private String numero;
        private String statut;
        private String couleur;
        private LocalDate date;
        private String ville;
    }

    /** Dernier signalement déclaré. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignalementRecent {
        private String reference;
        private String type;
        private String couleurType;
        private String statut;
        private LocalDateTime date;
    }

    /** Dernière action tracée dans le journal d'audit (hors lectures). */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiviteRecente {
        private String entite;
        private String action;
        private LocalDateTime date;
        private String utilisateur;
    }
}
