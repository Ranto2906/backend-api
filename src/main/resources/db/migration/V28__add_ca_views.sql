-- ══════════════════════════════════════════════════════════════
-- Vues de calcul du chiffre d'affaires (tableau de bord)
--   • vue_ca_theorique : TOUS les dossiers, tous statuts confondus
--   • vue_ca_reel      : uniquement les dossiers au statut TERMINÉ
--                        (statut_dossier.est_final = true)
-- Colonnes identiques dans les deux vues → requête de calcul commune,
-- filtrable par date de création (date_creation).
-- ══════════════════════════════════════════════════════════════

CREATE OR REPLACE VIEW vue_ca_theorique AS
SELECT d.id_dossier,
       d.numero_dossier,
       d.date_creation,
       d.annee,
       d.id_ville,
       COALESCE(v.nom_ville, '(aucune)')   AS ville,
       COALESCE(sd.libelle, 'Sans statut') AS statut,
       COALESCE(sd.est_final, false)       AS est_final,
       COALESCE(d.montant_total, 0)        AS montant
FROM dossier d
LEFT JOIN statut_dossier sd ON sd.id_statut_dossier = d.id_statut_dossier
LEFT JOIN ville v           ON v.id_ville = d.id_ville;

CREATE OR REPLACE VIEW vue_ca_reel AS
SELECT id_dossier,
       numero_dossier,
       date_creation,
       annee,
       id_ville,
       ville,
       statut,
       est_final,
       montant
FROM vue_ca_theorique
WHERE est_final;
