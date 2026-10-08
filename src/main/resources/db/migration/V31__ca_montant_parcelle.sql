-- ══════════════════════════════════════════════════════════════
-- V31 — CA calculé sur la même règle que la fiche dossier
-- ══════════════════════════════════════════════════════════════
--
-- PROBLÈME : les vues V28/V30 lisaient dossier.montant_total — une colonne
-- figée, souvent à 0 ou obsolète pour les dossiers importés (l'import ne la
-- renseigne que si la valeur TTC figure dans le fichier). Résultat : le CA
-- théorique (43,9 M pour 490 dossiers) était INFÉRIEUR au montant d'un SEUL
-- dossier (50,6 M : 422 m² × prix au m² de la ville, tel qu'affiché sur la
-- fiche du dossier).
--
-- CORRECTION : le montant d'un dossier est désormais calculé comme sur sa
-- fiche (DetailDemande + enregistrerParcelles) :
--     Σ (dossier_parcelle.superficie_m2 × dernier prix_m2 TTC de la ville)
-- avec repli sur dossier.montant_total (valeur importée) quand le calcul est
-- impossible (aucune parcelle, superficie absente, ou pas de prix pour la
-- ville). Le CA suit la grille tarifaire courante, comme l'affichage.

DROP VIEW IF EXISTS vue_ca_reel;
DROP VIEW IF EXISTS vue_ca_theorique;

CREATE VIEW vue_ca_theorique AS
SELECT d.id_dossier,
       d.numero_dossier,
       d.date_creation,
       d.annee,
       d.id_ville,
       COALESCE(v.nom_ville, '(aucune)')   AS ville,
       COALESCE(sd.libelle, 'Sans statut') AS statut,
       COALESCE(sd.code, NULL)             AS code_statut,
       COALESCE(sd.est_final, false)       AS est_final,
       COALESCE(
           (SELECT ROUND(sum(dp.superficie_m2 * pm.prix_ttc), 2)
            FROM dossier_parcelle dp
            JOIN LATERAL (
                SELECT p2.prix_ttc
                FROM prix_m2 p2
                WHERE p2.id_ville = d.id_ville
                ORDER BY p2.date_maj DESC
                LIMIT 1
            ) pm ON true
            WHERE dp.id_dossier = d.id_dossier
              AND dp.superficie_m2 IS NOT NULL),
           d.montant_total,
           0
       ) AS montant
FROM dossier d
LEFT JOIN statut_dossier sd ON sd.id_statut_dossier = d.id_statut_dossier
LEFT JOIN ville v           ON v.id_ville = d.id_ville;

CREATE VIEW vue_ca_reel AS
SELECT id_dossier,
       numero_dossier,
       date_creation,
       annee,
       id_ville,
       ville,
       statut,
       code_statut,
       est_final,
       montant
FROM vue_ca_theorique
WHERE code_statut = 'Termine';
