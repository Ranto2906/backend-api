-- ══════════════════════════════════════════════════════════════
-- V37 — CA reposant sur le MONTANT ENREGISTRÉ du dossier
-- ══════════════════════════════════════════════════════════════
--
-- PROBLÈME : la vue V31 recalculait le montant de CHAQUE dossier avec le
-- DERNIER prix au m² de la ville (prix courant) : modifier un prix faisait
-- bouger toute la synthèse financière (CA réel, CA théorique, répartitions),
-- y compris pour des dossiers déjà enregistrés.
--
-- CORRECTION : le montant d'un dossier est désormais celui enregistré sur le
-- dossier — dossier.montant_total, calculé avec le prix au m² FIGÉ à son
-- enregistrement (snapshot V36, prix_m2_ttc). Modifier un prix du référentiel
-- ne modifie donc plus AUCUN montant historique ; seules les nouvelles
-- demandes prennent le nouveau prix.
--
-- Replis (dossiers hérités / importés) :
--   1. dossier.montant_total non nul            → montant enregistré (figé)
--   2. sinon : Σ superficie × prix FIGÉ du dossier
--      (prix courant seulement si le dossier n'a aucun prix figé)
--   3. sinon : 0
--
-- Le CA réel (vue_ca_reel) est dérivé de cette vue, il suit automatiquement.

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
           NULLIF(d.montant_total, 0),
           (SELECT ROUND(sum(dp.superficie_m2 * COALESCE(d.prix_m2_ttc, pm.prix_ttc)), 2)
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

-- ============================================================================
-- FIN DE LA MIGRATION V37
-- ============================================================================
