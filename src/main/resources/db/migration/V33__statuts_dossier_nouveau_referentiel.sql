-- ══════════════════════════════════════════════════════════════
-- V33 — Nouveau référentiel des statuts de dossier
-- ══════════════════════════════════════════════════════════════
--
-- Remplace la liste « En cours / Validé / Rejeté / Annulé / Terminé »
-- par les 4 statuts métier : En cours / Terminé / En instance / Archivé.
--
-- Conservation des identifiants existants quand le statut correspond
-- déjà (En cours, Terminé) pour ne pas casser les lignes dossier qui
-- y font référence ; les autres lignes sont réaffectées, puis les
-- anciens statuts retirés (aucun dossier ne les utilisait).

-- 1) Mise à jour des statuts conservés (libellé/couleur/ordre harmonisés)
UPDATE statut_dossier SET libelle = 'En cours', couleur_hex = '#f0ad4e', ordre = 1, est_final = FALSE
WHERE code = 'En cours' AND (libelle <> 'En cours' OR couleur_hex <> '#f0ad4e' OR ordre <> 1);

UPDATE statut_dossier SET libelle = 'Terminé', couleur_hex = '#5cb85c', ordre = 2, est_final = TRUE
WHERE code = 'Termine' AND (libelle <> 'Terminé' OR couleur_hex <> '#5cb85c' OR ordre <> 2);

-- 2) Ajout des deux nouveaux statuts
INSERT INTO statut_dossier (code, libelle, couleur_hex, est_final, ordre)
SELECT 'En instance', 'En instance', '#f97316', FALSE, 3
WHERE NOT EXISTS (SELECT 1 FROM statut_dossier WHERE code = 'En instance');

INSERT INTO statut_dossier (code, libelle, couleur_hex, est_final, ordre)
SELECT 'Archive', 'Archivé', '#6b7280', TRUE, 4
WHERE NOT EXISTS (SELECT 1 FROM statut_dossier WHERE code = 'Archive');

-- 3) Réaffectation éventuelle des dossiers sur d'anciens statuts,
--    avant retrait (Valide → En cours ; Rejete/Annule → En instance)
UPDATE dossier d
SET id_statut_dossier = (SELECT id_statut_dossier FROM statut_dossier WHERE code = 'En cours')
WHERE id_statut_dossier IN (SELECT id_statut_dossier FROM statut_dossier WHERE code IN ('Valide'));

UPDATE dossier d
SET id_statut_dossier = (SELECT id_statut_dossier FROM statut_dossier WHERE code = 'En instance')
WHERE id_statut_dossier IN (SELECT id_statut_dossier FROM statut_dossier WHERE code IN ('Rejete', 'Annule'));

-- 4) Retrait des anciens statuts désormais inutilisés
DELETE FROM statut_dossier WHERE code IN ('Valide', 'Rejete', 'Annule');
