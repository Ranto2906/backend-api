package com.seimad.patrimoine.service.statistique;

import com.seimad.patrimoine.dto.statistique.TableauBordDTO;
import com.seimad.patrimoine.dto.statistique.TableauBordDTO.ActiviteRecente;
import com.seimad.patrimoine.dto.statistique.TableauBordDTO.AlerteTableauBord;
import com.seimad.patrimoine.dto.statistique.TableauBordDTO.Avancement;
import com.seimad.patrimoine.dto.statistique.TableauBordDTO.DossierRecent;
import com.seimad.patrimoine.dto.statistique.TableauBordDTO.Finances;
import com.seimad.patrimoine.dto.statistique.TableauBordDTO.GeometrieStats;
import com.seimad.patrimoine.dto.statistique.TableauBordDTO.Kpi;
import com.seimad.patrimoine.dto.statistique.TableauBordDTO.Repartition;
import com.seimad.patrimoine.dto.statistique.TableauBordDTO.RepartitionMontant;
import com.seimad.patrimoine.dto.statistique.TableauBordDTO.SerieMensuelle;
import com.seimad.patrimoine.dto.statistique.TableauBordDTO.SignalementRecent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Tableau de bord : agrège en une seule réponse toutes les statistiques du
 * patrimoine foncier (dossiers, terrains, notifications, signalements,
 * finances, cartographie, activité).
 *
 * <p>Les requêtes sont natives (PostgreSQL) et regroupées par section pour
 * limiter le nombre d'allers-retours. Le format des lignes est validé en base
 * (colonnes dans l'ordre attendu ci-dessous).</p>
 */
@Service
@Slf4j
public class TableauBordService {

    @PersistenceContext
    private EntityManager entityManager;

    /** Nombre de mois des séries temporelles sans filtre (12, mois courant compris). */
    private static final int MOIS_SERIE = 12;

    /** Plafond (en mois) des séries mensuelles lorsqu'une période est filtrée. */
    private static final int MOIS_SERIE_MAX = 36;

    // ─────────────────────────────────────────────────────────────
    // Requêtes (SQL validé sur la base de référence)
    // ─────────────────────────────────────────────────────────────

    /** 1 ligne : total, clôturés, en cours, année courante, montant total.
     *  Montant = vue_ca_theorique (superficie × dernier prix au m², repli
     *  montant_total) — même règle que la fiche dossier et la synthèse financière. */
    private static final String SQL_KPI_DOSSIERS = """
            SELECT count(*) AS total,
                   count(*) FILTER (WHERE sd.est_final) AS clotures,
                   count(*) FILTER (WHERE sd.est_final IS DISTINCT FROM TRUE) AS en_cours,
                   count(*) FILTER (WHERE d.annee = EXTRACT(YEAR FROM CURRENT_DATE)) AS annee_courante,
                   COALESCE(sum(vt.montant), 0) AS montant_total
            FROM dossier d
            LEFT JOIN statut_dossier sd ON sd.id_statut_dossier = d.id_statut_dossier
            LEFT JOIN vue_ca_theorique vt ON vt.id_dossier = d.id_dossier
            WHERE TRUE%s
            """;

    /** 1 ligne : propriétés, parcelles, référentiels, acteurs, alertes lues. */
    private static final String SQL_KPI_PATRIMOINE = """
            SELECT (SELECT count(*) FROM propriete) AS nb_proprietes,
                   (SELECT COALESCE(sum(superficie_totale), 0) FROM propriete) AS superficie_proprietes,
                   (SELECT count(*) FROM parcelle) AS nb_parcelles,
                   (SELECT COALESCE(sum(superficie_m2), 0) FROM parcelle) AS superficie_parcelles,
                   (SELECT count(*) FROM ville) AS nb_villes,
                   (SELECT count(*) FROM lieu) AS nb_lieux,
                   (SELECT count(*) FROM personne) AS nb_personnes,
                   (SELECT count(*) FROM utilisateur) AS nb_utilisateurs,
                   (SELECT count(*) FROM utilisateur WHERE actif) AS nb_utilisateurs_actifs,
                   (SELECT count(*) FROM alerte WHERE NOT lu) AS nb_alertes_non_lues,
                   (SELECT count(*) FROM dossier_demandeur) AS nb_demandeurs,
                   (SELECT count(*) FROM document_dossier) AS nb_documents
            """;

    /** 1 ligne : notifications, avertissements, signalements, descentes, suivis (%1$s..%4$s = période). */
    private static final String SQL_KPI_SUIVI = """
            SELECT (SELECT count(*) FROM notification_occupation WHERE TRUE%1$s) AS notifications,
                   (SELECT count(*) FROM notification_occupation WHERE statut = 'En cours'%1$s) AS notifications_en_cours,
                   (SELECT count(*) FROM avertissement WHERE TRUE%2$s) AS avertissements,
                   (SELECT count(*) FROM signalement WHERE TRUE%3$s) AS signalements,
                   (SELECT count(*) FROM signalement s
                    LEFT JOIN statut_signalement ss ON ss.id_statut_signalement = s.id_statut_signalement
                    WHERE ss.est_final IS DISTINCT FROM TRUE%3$s) AS signalements_ouverts,
                   (SELECT count(*) FROM descente_terrain WHERE TRUE%4$s) AS descentes,
                   (SELECT count(*) FROM descente_terrain WHERE validation = 'En attente'%4$s) AS descentes_attente,
                   (SELECT count(*) FROM suivi_notification) AS suivis_notification
            """;

    /** 1 ligne : nombre de géométries, superficie tracée, propriétés localisées. */
    private static final String SQL_KPI_GEOMETRIE = """
            SELECT count(*) AS nb,
                   COALESCE(sum(superficie_calculee_m2), 0) AS superficie,
                   count(*) FILTER (WHERE entite_type = 'propriete') AS localisees
            FROM geometrie
            """;

    /** 1 ligne : propriétés sans géométrie (convention UUID 00000000-…-{id:012d}). */
    private static final String SQL_PROPRIETES_SANS_GEOMETRIE = """
            SELECT count(*)
            FROM propriete p
            WHERE NOT EXISTS (
                SELECT 1 FROM geometrie g
                WHERE g.entite_type = 'propriete'
                  AND g.entite_id = ('00000000-0000-0000-0000-' || lpad(p.id_propriete::text, 12, '0'))::uuid
            )
            """;

    /** 1 ligne : étapes, suivis, suivis réalisés, dossiers suivis. */
    private static final String SQL_AVANCEMENT = """
            SELECT (SELECT count(*) FROM etape) AS etapes,
                   (SELECT count(*) FROM suivi_dossier) AS suivis,
                   (SELECT count(*) FROM suivi_dossier WHERE date_realisation IS NOT NULL) AS realises,
                   (SELECT count(DISTINCT id_dossier) FROM suivi_dossier) AS dossiers_suivis
            """;

    private static final String SQL_STATUTS_DOSSIER = """
            SELECT COALESCE(sd.libelle, 'Sans statut') AS libelle,
                   COALESCE(sd.couleur_hex, '#94a3b8') AS couleur,
                   count(*) AS valeur
            FROM dossier d
            LEFT JOIN statut_dossier sd ON sd.id_statut_dossier = d.id_statut_dossier
            WHERE TRUE%s
            GROUP BY COALESCE(sd.libelle, 'Sans statut'), COALESCE(sd.couleur_hex, '#94a3b8')
            ORDER BY valeur DESC
            """;

    /** Libellé NULL conservé : le service signale les dossiers sans type. */
    private static final String SQL_TYPES_DOSSIER = """
            SELECT td.libelle AS libelle, count(*) AS valeur
            FROM dossier d
            LEFT JOIN type_dossier td ON td.id_type_dossier = d.id_type_dossier
            WHERE TRUE%s
            GROUP BY td.libelle
            ORDER BY valeur DESC
            """;

    private static final String SQL_STATUTS_SIGNALEMENT = """
            SELECT COALESCE(ss.libelle, 'Sans statut') AS libelle,
                   COALESCE(ss.couleur_hex, '#94a3b8') AS couleur,
                   count(s.id_signalement) AS valeur
            FROM statut_signalement ss
            LEFT JOIN signalement s ON s.id_statut_signalement = ss.id_statut_signalement%s
            GROUP BY COALESCE(ss.libelle, 'Sans statut'), COALESCE(ss.couleur_hex, '#94a3b8')
            ORDER BY COALESCE(min(ss.ordre), 999)
            """;

    private static final String SQL_TYPES_SIGNALEMENT = """
            SELECT COALESCE(ts.libelle, 'Sans type') AS libelle,
                   COALESCE(ts.couleur, '#94a3b8') AS couleur,
                   count(s.id_signalement) AS valeur
            FROM type_signalement ts
            LEFT JOIN signalement s ON s.id_type_signalement = ts.id_type_signalement%s
            GROUP BY COALESCE(ts.libelle, 'Sans type'), COALESCE(ts.couleur, '#94a3b8')
            ORDER BY valeur DESC
            """;

    private static final String SQL_STATUTS_NOTIFICATION = """
            SELECT COALESCE(statut, 'Sans statut') AS libelle, count(*) AS valeur
            FROM notification_occupation
            WHERE TRUE%s
            GROUP BY COALESCE(statut, 'Sans statut')
            ORDER BY valeur DESC
            """;

    private static final String SQL_VALIDATIONS_DESCENTE = """
            SELECT COALESCE(validation, 'Sans statut') AS libelle, count(*) AS valeur
            FROM descente_terrain
            WHERE TRUE%s
            GROUP BY COALESCE(validation, 'Sans statut')
            ORDER BY valeur DESC
            """;

    private static final String SQL_CONSTATS_DESCENTE = """
            SELECT COALESCE(statut_constat, 'Sans constat') AS libelle, count(*) AS valeur
            FROM descente_terrain
            WHERE TRUE%s
            GROUP BY COALESCE(statut_constat, 'Sans constat')
            ORDER BY valeur DESC
            """;

    /** Top 8 des villes par nombre de propriétés rattachées. */
    private static final String SQL_PROPRIETES_PAR_VILLE = """
            SELECT COALESCE(v.nom_ville, 'Non rattachée') AS libelle, count(*) AS valeur
            FROM propriete p
            LEFT JOIN lieu l ON l.id_lieu = p.id_lieu
            LEFT JOIN ville v ON v.id_ville = l.id_ville
            GROUP BY COALESCE(v.nom_ville, 'Non rattachée')
            ORDER BY valeur DESC
            LIMIT 8
            """;

    private static final String SQL_GEOMETRIES_PAR_TYPE = """
            SELECT entite_type AS libelle, count(*) AS valeur
            FROM geometrie
            GROUP BY entite_type
            ORDER BY valeur DESC
            """;

    /** Lignes « YYYY-MM » : dossiers créés par mois (%1$s/%2$s = bornes de période). */
    private static final String SQL_SERIE_DOSSIERS = """
            SELECT to_char(g.m, 'YYYY-MM') AS periode, count(d.id_dossier) AS valeur
            FROM generate_series(%1$s, %2$s, interval '1 month') g(m)
            LEFT JOIN dossier d ON date_trunc('month', d.date_creation) = g.m
            GROUP BY g.m
            ORDER BY g.m
            """;

    /** Lignes « YYYY-MM » : notifications créées par mois (%1$s/%2$s = bornes de période). */
    private static final String SQL_SERIE_NOTIFICATIONS = """
            SELECT to_char(g.m, 'YYYY-MM') AS periode, count(n.id_notification) AS valeur
            FROM generate_series(%1$s, %2$s, interval '1 month') g(m)
            LEFT JOIN notification_occupation n ON date_trunc('month', n.created_at) = g.m
            GROUP BY g.m
            ORDER BY g.m
            """;

    /** Lignes « YYYY-MM » : signalements déposés par mois (%1$s/%2$s = bornes de période). */
    private static final String SQL_SERIE_SIGNALEMENTS = """
            SELECT to_char(g.m, 'YYYY-MM') AS periode, count(s.id_signalement) AS valeur
            FROM generate_series(%1$s, %2$s, interval '1 month') g(m)
            LEFT JOIN signalement s ON date_trunc('month', s.date_signalement) = g.m
            GROUP BY g.m
            ORDER BY g.m
            """;

    /** 6 lignes : numero, statut, couleur, date de création, ville. */
    private static final String SQL_DOSSIERS_RECENTS = """
            SELECT d.numero_dossier,
                   COALESCE(sd.libelle, 'Sans statut'),
                   COALESCE(sd.couleur_hex, '#94a3b8'),
                   d.date_creation,
                   COALESCE(v.nom_ville, '-')
            FROM dossier d
            LEFT JOIN statut_dossier sd ON sd.id_statut_dossier = d.id_statut_dossier
            LEFT JOIN ville v ON v.id_ville = d.id_ville
            WHERE TRUE%s
            ORDER BY d.date_creation DESC
            LIMIT 6
            """;

    /** 6 lignes : référence, type, couleur type, statut, date. */
    private static final String SQL_SIGNALEMENTS_RECENTS = """
            SELECT s.reference,
                   COALESCE(ts.libelle, '-'),
                   COALESCE(ts.couleur, '#94a3b8'),
                   COALESCE(ss.libelle, 'Sans statut'),
                   s.date_signalement
            FROM signalement s
            LEFT JOIN type_signalement ts ON ts.id_type_signalement = s.id_type_signalement
            LEFT JOIN statut_signalement ss ON ss.id_statut_signalement = s.id_statut_signalement
            WHERE TRUE%s
            ORDER BY s.date_signalement DESC
            LIMIT 6
            """;

    /** 8 lignes d'audit hors lectures (les READ noyeraient la synthèse). */
    private static final String SQL_ACTIVITE_RECENTE = """
            SELECT a.entite_type, a.action, a.date_action,
                   COALESCE(NULLIF(u.nom_complet, ''), u.nom_utilisateur)
            FROM audit a
            LEFT JOIN utilisateur u ON u.id_utilisateur = a.id_utilisateur
            WHERE a.action <> 'READ'%s
            ORDER BY a.date_action DESC
            LIMIT 8
            """;

    /** Toutes les villes : montant total (vue CA), nombre de dossiers (sans LIMIT —
     *  la synthèse financière doit totaliser le CA de la période). */
    private static final String SQL_MONTANT_PAR_VILLE = """
            SELECT COALESCE(vt.ville, '(aucune)') AS libelle,
                   COALESCE(sum(vt.montant), 0) AS montant,
                   count(*) AS nombre
            FROM dossier d
            LEFT JOIN vue_ca_theorique vt ON vt.id_dossier = d.id_dossier
            WHERE TRUE%s
            GROUP BY COALESCE(vt.ville, '(aucune)')
            ORDER BY montant DESC, nombre DESC
            """;

    /** Toutes les années (+ « (sans année) ») : montant total (vue CA), nombre de
     *  dossiers. Sans la ligne NULL, la somme ne recouperait pas le CA total. */
    private static final String SQL_MONTANT_PAR_ANNEE = """
            SELECT COALESCE(vt.annee::text, '(sans année)') AS libelle,
                   COALESCE(sum(vt.montant), 0) AS montant,
                   count(*) AS nombre
            FROM dossier d
            LEFT JOIN vue_ca_theorique vt ON vt.id_dossier = d.id_dossier
            WHERE TRUE%s
            GROUP BY COALESCE(vt.annee::text, '(sans année)')
            ORDER BY libelle DESC
            """;

    /** 1 ligne : CA réel — vue des seuls dossiers au statut terminé (est_final). */
    private static final String SQL_CA_REEL = """
            SELECT COALESCE(sum(montant), 0) AS ca_reel, count(*) AS nb_dossiers
            FROM vue_ca_reel
            WHERE TRUE%s
            """;

    /** 1 ligne : CA théorique — vue de TOUS les dossiers, tous statuts confondus. */
    private static final String SQL_CA_THEORIQUE = """
            SELECT COALESCE(sum(montant), 0) AS ca_theorique, count(*) AS nb_dossiers
            FROM vue_ca_theorique
            WHERE TRUE%s
            """;

    // ─────────────────────────────────────────────────────────────
    // Filtre de période (paramètres dateDebut / dateFin)
    // ─────────────────────────────────────────────────────────────

    /**
     * Condition SQL de période sur une colonne datée : chaîne vide sans filtre,
     * sinon {@code AND colonne >= début} puis {@code AND colonne < fin + 1 jour}
     * (fin incluse). Seules des {@link LocalDate} sérialisées en ISO sont
     * concaténées — jamais de saisie utilisateur.
     */
    private static String periode(String colonne, LocalDate debut, LocalDate fin) {
        StringBuilder sb = new StringBuilder();
        if (debut != null) {
            sb.append(" AND ").append(colonne).append(" >= DATE '").append(debut).append("'");
        }
        if (fin != null) {
            sb.append(" AND ").append(colonne).append(" < DATE '").append(fin).append("' + INTERVAL '1 day'");
        }
        return sb.toString();
    }

    /**
     * Bornes des séries mensuelles : 12 derniers mois sans filtre, sinon la
     * période demandée plafonnée à {@link #MOIS_SERIE_MAX} mois. Retourne deux
     * expressions SQL {@code date_trunc('month', DATE 'YYYY-MM-01')}.
     */
    private static String[] bornesSerie(LocalDate debut, LocalDate fin) {
        YearMonth courant = YearMonth.now();
        YearMonth finM = fin != null ? YearMonth.from(fin) : courant;
        YearMonth debutM = debut != null ? YearMonth.from(debut) : finM.minusMonths(MOIS_SERIE - 1);
        if (debutM.isAfter(finM)) {                 // période inversée : on rétablit l'ordre
            YearMonth tmp = debutM;
            debutM = finM;
            finM = tmp;
        }
        if (debutM.isBefore(finM.minusMonths(MOIS_SERIE_MAX - 1))) {
            debutM = finM.minusMonths(MOIS_SERIE_MAX - 1);
        }
        return new String[]{
                "date_trunc('month', DATE '" + debutM.atDay(1) + "')",
                "date_trunc('month', DATE '" + finM.atDay(1) + "')"
        };
    }

    // ─────────────────────────────────────────────────────────────
    // Lecture
    // ─────────────────────────────────────────────────────────────

    /**
     * @param dateDebut première date du filtre, incluse (null = sans limite)
     * @param dateFin   dernière date du filtre, incluse (null = sans limite)
     */
    @Transactional(readOnly = true)
    public TableauBordDTO tableauBord(LocalDate dateDebut, LocalDate dateFin) {
        // Conditions de période : chaîne vide quand aucun filtre n'est posé.
        String pDossier = periode("d.date_creation", dateDebut, dateFin);
        String pSignalement = periode("date_signalement", dateDebut, dateFin);
        String pNotification = periode("created_at", dateDebut, dateFin);
        String pDescente = periode("COALESCE(date_descente, created_at)", dateDebut, dateFin);
        String[] mois = bornesSerie(dateDebut, dateFin);

        Object[] dossiers = uneLigne(SQL_KPI_DOSSIERS.formatted(pDossier));
        Kpi kpi = lireKpi(dossiers, dateDebut, dateFin);
        Avancement avancement = lireAvancement(nombre(dossiers, 0));
        GeometrieStats geometrie = lireGeometrie(kpi.getNbProprietes());
        Finances finances = lireFinances(kpi.getMontantTotal(), dateDebut, dateFin);

        List<Repartition> dossiersParStatut = repartitionsAvecCouleur(SQL_STATUTS_DOSSIER.formatted(pDossier));
        List<Repartition> dossiersParType = repartitionsAvecSans(SQL_TYPES_DOSSIER.formatted(pDossier), "Non défini");

        TableauBordDTO.TableauBordDTOBuilder builder = TableauBordDTO.builder()
                .genereLe(LocalDateTime.now())
                .kpi(kpi)
                .avancement(avancement)
                .geometrie(geometrie)
                .finances(finances)
                .dossiersParMois(series(SQL_SERIE_DOSSIERS.formatted(mois[0], mois[1])))
                .notificationsParMois(series(SQL_SERIE_NOTIFICATIONS.formatted(mois[0], mois[1])))
                .signalementsParMois(series(SQL_SERIE_SIGNALEMENTS.formatted(mois[0], mois[1])))
                .dossiersParStatut(dossiersParStatut)
                .dossiersParType(dossiersParType)
                .signalementsParStatut(repartitionsAvecCouleur(SQL_STATUTS_SIGNALEMENT.formatted(pSignalement)))
                .signalementsParType(repartitionsAvecCouleur(SQL_TYPES_SIGNALEMENT.formatted(pSignalement)))
                .notificationsParStatut(repartitionsSimples(SQL_STATUTS_NOTIFICATION.formatted(pNotification)))
                .descentesParValidation(repartitionsSimples(SQL_VALIDATIONS_DESCENTE.formatted(pDescente)))
                .descentesParConstat(repartitionsSimples(SQL_CONSTATS_DESCENTE.formatted(pDescente)))
                .proprietesParVille(repartitionsSimples(SQL_PROPRIETES_PAR_VILLE))
                .geometriesParType(repartitionsSimples(SQL_GEOMETRIES_PAR_TYPE))
                .derniersDossiers(derniersDossiers(dateDebut, dateFin))
                .derniersSignalements(derniersSignalements(dateDebut, dateFin))
                .activiteRecente(activiteRecente(dateDebut, dateFin));

        long sansType = dossiersParType.stream()
                .filter(r -> "Non défini".equals(r.getLibelle()))
                .mapToLong(Repartition::getValeur)
                .sum();
        builder.alertes(construireAlertes(kpi, geometrie, sansType));

        return builder.build();
    }

    // ─────────────────────────────────────────────────────────────
    // Sections
    // ─────────────────────────────────────────────────────────────

    private Kpi lireKpi(Object[] dossiers, LocalDate dateDebut, LocalDate dateFin) {
        Object[] patrimoine = uneLigne(SQL_KPI_PATRIMOINE);
        Object[] suivi = uneLigne(SQL_KPI_SUIVI.formatted(
                periode("created_at", dateDebut, dateFin),
                periode("date_avertissement", dateDebut, dateFin),
                periode("date_signalement", dateDebut, dateFin),
                periode("COALESCE(date_descente, created_at)", dateDebut, dateFin)));
        Object[] carte = uneLigne(SQL_KPI_GEOMETRIE);

        return Kpi.builder()
                .nbDossiers(nombre(dossiers, 0))
                .nbDossiersClotures(nombre(dossiers, 1))
                .nbDossiersEnCours(nombre(dossiers, 2))
                .nbDossiersAnneeCourante(nombre(dossiers, 3))
                .montantTotal(decimale(dossiers, 4))
                .nbProprietes(nombre(patrimoine, 0))
                .superficieProprietes(decimale(patrimoine, 1))
                .nbParcelles(nombre(patrimoine, 2))
                .superficieParcelles(decimale(patrimoine, 3))
                .nbVilles(nombre(patrimoine, 4))
                .nbLieux(nombre(patrimoine, 5))
                .nbPersonnes(nombre(patrimoine, 6))
                .nbUtilisateurs(nombre(patrimoine, 7))
                .nbUtilisateursActifs(nombre(patrimoine, 8))
                .nbAlertesNonLues(nombre(patrimoine, 9))
                .nbDemandeurs(nombre(patrimoine, 10))
                .nbDocuments(nombre(patrimoine, 11))
                .nbNotifications(nombre(suivi, 0))
                .nbNotificationsEnCours(nombre(suivi, 1))
                .nbAvertissements(nombre(suivi, 2))
                .nbSignalements(nombre(suivi, 3))
                .nbSignalementsOuverts(nombre(suivi, 4))
                .nbDescentes(nombre(suivi, 5))
                .nbDescentesEnAttente(nombre(suivi, 6))
                // index 7 = suivis de notification (non exposé en KPI)
                .build();
    }

    private Avancement lireAvancement(long nbDossiers) {
        Object[] l = uneLigne(SQL_AVANCEMENT);
        long etapes = nombre(l, 0);
        long suivis = nombre(l, 1);
        long realises = nombre(l, 2);
        long dossiersSuivis = nombre(l, 3);

        long attendus = nbDossiers * etapes;
        return Avancement.builder()
                .nbEtapes(etapes)
                .nbSuivis(suivis)
                .nbSuivisRealises(realises)
                .nbDossiersSuivis(dossiersSuivis)
                .tauxRealisation(pourcent(realises, suivis))
                .tauxRemplissage(pourcent(suivis, attendus))
                .build();
    }

    private GeometrieStats lireGeometrie(long nbProprietes) {
        Object[] carte = uneLigne(SQL_KPI_GEOMETRIE);
        long localisees = nombre(carte, 2);
        long sansGeometrie = scalaireEntier(SQL_PROPRIETES_SANS_GEOMETRIE);

        return GeometrieStats.builder()
                .nbGeometries(nombre(carte, 0))
                .superficieTotale(decimale(carte, 1))
                .nbProprietesLocalisees(localisees)
                .nbProprietesSansGeometrie(sansGeometrie)
                .couverturePourcent(pourcent(localisees, nbProprietes))
                .build();
    }

    private Finances lireFinances(BigDecimal montantTotal, LocalDate dateDebut, LocalDate dateFin) {
        String pDossier = periode("d.date_creation", dateDebut, dateFin);
        String pVue = periode("date_creation", dateDebut, dateFin);

        // CA réel / CA théorique — vues SQL (statuts terminés vs tous statuts).
        BigDecimal caReel = decimale(uneLigne(SQL_CA_REEL.formatted(pVue)), 0);
        BigDecimal caTheorique = decimale(uneLigne(SQL_CA_THEORIQUE.formatted(pVue)), 0);
        BigDecimal ecart = caTheorique.subtract(caReel);
        double taux = caTheorique.signum() > 0
                ? caReel.multiply(BigDecimal.valueOf(100))
                        .divide(caTheorique, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        List<RepartitionMontant> parVille = new ArrayList<>();
        for (Object[] l : lignes(SQL_MONTANT_PAR_VILLE.formatted(pDossier))) {
            parVille.add(RepartitionMontant.builder()
                    .libelle(texte(l, 0))
                    .montant(decimale(l, 1))
                    .nombre(nombre(l, 2))
                    .build());
        }
        List<RepartitionMontant> parAnnee = new ArrayList<>();
        for (Object[] l : lignes(SQL_MONTANT_PAR_ANNEE.formatted(pDossier))) {
            parAnnee.add(RepartitionMontant.builder()
                    .libelle(texte(l, 0))
                    .montant(decimale(l, 1))
                    .nombre(nombre(l, 2))
                    .build());
        }
        return Finances.builder()
                .montantTotal(montantTotal)
                .caReel(caReel)
                .caTheorique(caTheorique)
                .ecart(ecart)
                .tauxRealisation(taux)
                .parVille(parVille)
                .parAnnee(parAnnee)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // Alertes (calculées à partir des compteurs)
    // ─────────────────────────────────────────────────────────────

    private List<AlerteTableauBord> construireAlertes(Kpi kpi, GeometrieStats geo, long dossiersSansType) {
        List<AlerteTableauBord> alertes = new ArrayList<>();
        if (kpi.getNbAlertesNonLues() > 0) {
            alertes.add(AlerteTableauBord.builder().niveau("danger")
                    .message(pluriel(kpi.getNbAlertesNonLues(), "alerte non lue", "alertes non lues")).build());
        }
        if (kpi.getNbSignalementsOuverts() > 0) {
            alertes.add(AlerteTableauBord.builder().niveau("warning")
                    .message(pluriel(kpi.getNbSignalementsOuverts(),
                            "signalement en attente de traitement",
                            "signalements en attente de traitement")).build());
        }
        if (kpi.getNbDescentesEnAttente() > 0) {
            alertes.add(AlerteTableauBord.builder().niveau("warning")
                    .message(pluriel(kpi.getNbDescentesEnAttente(),
                            "descente terrain en attente de validation",
                            "descentes terrain en attente de validation")).build());
        }
        if (geo.getNbProprietesSansGeometrie() > 0) {
            alertes.add(AlerteTableauBord.builder().niveau("warning")
                    .message(pluriel(geo.getNbProprietesSansGeometrie(),
                            "propriété sans géométrie sur la carte",
                            "propriétés sans géométrie sur la carte")
                            + " (" + geo.getNbProprietesSansGeometrie() + "/" + kpi.getNbProprietes() + ")").build());
        }
        if (dossiersSansType > 0) {
            alertes.add(AlerteTableauBord.builder().niveau("info")
                    .message(pluriel(dossiersSansType, "dossier sans type renseigné",
                            "dossiers sans type renseigné")).build());
        }
        if (kpi.getNbNotificationsEnCours() > 0) {
            alertes.add(AlerteTableauBord.builder().niveau("info")
                    .message(pluriel(kpi.getNbNotificationsEnCours(),
                            "notification d'occupation en cours",
                            "notifications d'occupation en cours")).build());
        }
        if (kpi.getNbDossiersEnCours() > 0) {
            alertes.add(AlerteTableauBord.builder().niveau("info")
                    .message(pluriel(kpi.getNbDossiersEnCours(), "dossier en cours de traitement",
                            "dossiers en cours de traitement")).build());
        }
        return alertes;
    }

    private static String pluriel(long n, String singulier, String pluriel) {
        return n + " " + (n > 1 ? pluriel : singulier);
    }

    // ─────────────────────────────────────────────────────────────
    // Conversion des lignes
    // ─────────────────────────────────────────────────────────────

    /** Répartitions avec couleur (colonnes : libellé, couleur, valeur). */
    private List<Repartition> repartitionsAvecCouleur(String sql) {
        List<Repartition> resultat = new ArrayList<>();
        for (Object[] l : lignes(sql)) {
            resultat.add(Repartition.builder()
                    .libelle(texte(l, 0))
                    .couleur(texte(l, 1))
                    .valeur(nombre(l, 2))
                    .build());
        }
        completerPourcent(resultat);
        return resultat;
    }

    /** Répartitions simples (colonnes : libellé, valeur), sans couleur. */
    private List<Repartition> repartitionsSimples(String sql) {
        List<Repartition> resultat = new ArrayList<>();
        for (Object[] l : lignes(sql)) {
            resultat.add(Repartition.builder()
                    .libelle(texte(l, 0))
                    .valeur(nombre(l, 1))
                    .build());
        }
        completerPourcent(resultat);
        return resultat;
    }

    /**
     * Répartitions simples dont le libellé peut être NULL (colonnes : libellé,
     * valeur) — le libellé manquant est remplacé par {@code sansLibelle}.
     */
    private List<Repartition> repartitionsAvecSans(String sql, String sansLibelle) {
        List<Repartition> resultat = new ArrayList<>();
        for (Object[] l : lignes(sql)) {
            String libelle = texte(l, 0);
            resultat.add(Repartition.builder()
                    .libelle(libelle == null ? sansLibelle : libelle)
                    .valeur(nombre(l, 1))
                    .build());
        }
        completerPourcent(resultat);
        return resultat;
    }

    private static void completerPourcent(List<Repartition> repartitions) {
        long total = repartitions.stream().mapToLong(Repartition::getValeur).sum();
        repartitions.forEach(r -> r.setPourcent(pourcent(r.getValeur(), total)));
    }

    /** Série mensuelle « YYYY-MM » → valeur. */
    private List<SerieMensuelle> series(String sql) {
        List<SerieMensuelle> resultat = new ArrayList<>();
        for (Object[] l : lignes(sql)) {
            resultat.add(SerieMensuelle.builder().periode(texte(l, 0)).valeur(nombre(l, 1)).build());
        }
        return resultat;
    }

    private List<DossierRecent> derniersDossiers(LocalDate dateDebut, LocalDate dateFin) {
        List<DossierRecent> resultat = new ArrayList<>();
        for (Object[] l : lignes(SQL_DOSSIERS_RECENTS.formatted(periode("d.date_creation", dateDebut, dateFin)))) {
            resultat.add(DossierRecent.builder()
                    .numero(texte(l, 0))
                    .statut(texte(l, 1))
                    .couleur(texte(l, 2))
                    .date(date(l[3]))
                    .ville(texte(l, 4))
                    .build());
        }
        return resultat;
    }

    private List<SignalementRecent> derniersSignalements(LocalDate dateDebut, LocalDate dateFin) {
        List<SignalementRecent> resultat = new ArrayList<>();
        for (Object[] l : lignes(SQL_SIGNALEMENTS_RECENTS.formatted(periode("date_signalement", dateDebut, dateFin)))) {
            resultat.add(SignalementRecent.builder()
                    .reference(texte(l, 0))
                    .type(texte(l, 1))
                    .couleurType(texte(l, 2))
                    .statut(texte(l, 3))
                    .date(horodatage(l[4]))
                    .build());
        }
        return resultat;
    }

    private List<ActiviteRecente> activiteRecente(LocalDate dateDebut, LocalDate dateFin) {
        List<ActiviteRecente> resultat = new ArrayList<>();
        for (Object[] l : lignes(SQL_ACTIVITE_RECENTE.formatted(periode("a.date_action", dateDebut, dateFin)))) {
            resultat.add(ActiviteRecente.builder()
                    .entite(texte(l, 0))
                    .action(texte(l, 1))
                    .date(horodatage(l[2]))
                    .utilisateur(texte(l, 3))
                    .build());
        }
        return resultat;
    }

    // ─────────────────────────────────────────────────────────────
    // Accès JDBC (lignes brutes, sans entité JPA)
    // ─────────────────────────────────────────────────────────────

    private List<Object[]> lignes(String sql) {
        List<Object[]> resultat = new ArrayList<>();
        for (Object ligne : entityManager.createNativeQuery(sql).getResultList()) {
            if (ligne instanceof Object[] tableau) {
                resultat.add(tableau);
            }
        }
        return resultat;
    }

    /** Première ligne d'une requête, ou un tableau vide. */
    private Object[] uneLigne(String sql) {
        List<Object[]> l = lignes(sql);
        return l.isEmpty() ? new Object[0] : l.get(0);
    }

    private long scalaireEntier(String sql) {
        List<Object> l = entityManager.createNativeQuery(sql).getResultList();
        return l.isEmpty() ? 0 : nombre(l.get(0));
    }

    // ─────────────────────────────────────────────────────────────
    // Conversion tolérante des valeurs JDBC
    // ─────────────────────────────────────────────────────────────

    private static long nombre(Object[] ligne, int index) {
        return index < ligne.length ? nombre(ligne[index]) : 0L;
    }

    private static long nombre(Object valeur) {
        if (valeur instanceof Number n) return n.longValue();
        if (valeur == null) return 0L;
        try {
            return (long) Double.parseDouble(valeur.toString().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static BigDecimal decimale(Object[] ligne, int index) {
        if (index >= ligne.length || ligne[index] == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(ligne[index].toString().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static String texte(Object[] ligne, int index) {
        if (index >= ligne.length || ligne[index] == null) return null;
        String v = ligne[index].toString().trim();
        return v.isEmpty() ? null : v;
    }

    /** % arrondi à une décimale, 0 si le dénominateur est nul. */
    private static double pourcent(long numerateur, long denominateur) {
        if (denominateur <= 0) return 0.0;
        return BigDecimal.valueOf(numerateur * 100.0 / denominateur)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static java.time.LocalDate date(Object valeur) {
        if (valeur instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        if (valeur instanceof java.time.LocalDateTime ldt) return ldt.toLocalDate();
        if (valeur instanceof java.time.LocalDate ld) return ld;
        if (valeur instanceof java.sql.Date d) return d.toLocalDate();
        return null;
    }

    private static LocalDateTime horodatage(Object valeur) {
        if (valeur instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        if (valeur instanceof java.time.LocalDateTime ldt) return ldt;
        if (valeur instanceof java.time.OffsetDateTime odt) return odt.toLocalDateTime();
        if (valeur instanceof java.time.Instant i) return LocalDateTime.ofInstant(i, java.time.ZoneId.systemDefault());
        if (valeur instanceof java.time.LocalDate ld) return ld.atStartOfDay();
        return null;
    }
}
