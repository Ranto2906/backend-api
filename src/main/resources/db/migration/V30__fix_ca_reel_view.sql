-- ══════════════════════════════════════════════════════════════
-- V30 — Correction des vues de calcul du chiffre d'affaires
-- ══════════════════════════════════════════════════════════════
--
-- PROBLÈME (V28) : vue_ca_reel filtrait sur statut_dossier.est_final = true.
-- Or les statuts « finaux » sont : Terminé, REJETÉ et ANNULÉ (V3). Un dossier
-- rejeté ou annulé n'encaisse aucun montant : le compter dans le CA réel
-- gonflait artificiellement le CA, le taux de réalisation et faussait l'écart.
--
-- CORRECTION :
--  • vue_ca_theorique expose désormais le code du statut (code_statut) en plus
--    du libellé, pour filtrer de façon stable (le libellé peut être renommé).
--  • vue_ca_reel = dossiers TERMINÉ uniquement (code_statut = 'Termine').
--    À ajuster ici si un autre statut doit un jour compter comme encaissé.
--
-- NB : PostgreSQL refuse CREATE OR REPLACE VIEW si la liste des colonnes
-- change d'ordre ou de nom (erreur « ne peut pas modifier le nom de la
-- colonne »). Les vues ne stockent aucune donnée → DROP puis CREATE, en
-- supprimant d'abord vue_ca_reel qui en dépend.

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
       COALESCE(d.montant_total, 0)        AS montant
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
