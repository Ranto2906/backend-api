-- ══════════════════════════════════════════════════════════════
-- V32 — Workflow d'étapes pour le suivi des notifications
-- ══════════════════════════════════════════════════════════════
--
-- OBJECTIF : donner au suivi de notification un changement d'étape
-- similaire à celui du dossier (DetailDemande) :
--   - un référentiel d'étapes ordonnées (etape_notification),
--   - un suivi rattaché à une étape avec une date de réalisation,
--   - une couleur d'avancement réutilisant le référentiel
--     existant statut_couleur (Vert/Jaune/Rouge/...),
--   - un commentaire de changement d'étape.
--
-- Les colonnes ajoutées sur suivi_notification sont NULLABLE :
-- les suivis créés avant cette migration restent valables
-- (« étape libre ») et l'insertion Excel ne casse pas.

CREATE TABLE etape_notification (
   id_etape_notification INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   code_etape            VARCHAR(50)  NOT NULL UNIQUE,
   libelle               VARCHAR(150) NOT NULL,
   ordre                 INTEGER      NOT NULL UNIQUE,
   est_final             BOOLEAN      NOT NULL DEFAULT FALSE,
   created_at            TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- Cycle métier observé dans « Suivi des notifications Patrimoine foncier.xlsx »
INSERT INTO etape_notification (code_etape, libelle, ordre, est_final)
SELECT 'NOTIFICATION', 'Notification d''occupation', 1, FALSE
WHERE NOT EXISTS (SELECT 1 FROM etape_notification WHERE code_etape = 'NOTIFICATION');

INSERT INTO etape_notification (code_etape, libelle, ordre, est_final)
SELECT 'CONVOCATION', 'Convocation de l''occupant', 2, FALSE
WHERE NOT EXISTS (SELECT 1 FROM etape_notification WHERE code_etape = 'CONVOCATION');

INSERT INTO etape_notification (code_etape, libelle, ordre, est_final)
SELECT 'CONSTAT', 'Constat sur site', 3, FALSE
WHERE NOT EXISTS (SELECT 1 FROM etape_notification WHERE code_etape = 'CONSTAT');

INSERT INTO etape_notification (code_etape, libelle, ordre, est_final)
SELECT 'SOMATION', 'Somation / DJC', 4, FALSE
WHERE NOT EXISTS (SELECT 1 FROM etape_notification WHERE code_etape = 'SOMATION');

INSERT INTO etape_notification (code_etape, libelle, ordre, est_final)
SELECT 'ENLEVEMENT', 'Enlèvement de la construction', 5, FALSE
WHERE NOT EXISTS (SELECT 1 FROM etape_notification WHERE code_etape = 'ENLEVEMENT');

INSERT INTO etape_notification (code_etape, libelle, ordre, est_final)
SELECT 'CLOTURE', 'Clôture du dossier de notification', 6, TRUE
WHERE NOT EXISTS (SELECT 1 FROM etape_notification WHERE code_etape = 'CLOTURE');

ALTER TABLE suivi_notification
   ADD COLUMN id_etape_notification INTEGER REFERENCES etape_notification(id_etape_notification);

ALTER TABLE suivi_notification
   ADD COLUMN id_statut_couleur INTEGER REFERENCES statut_couleur(id_statut_couleur);

ALTER TABLE suivi_notification
   ADD COLUMN commentaire TEXT;
