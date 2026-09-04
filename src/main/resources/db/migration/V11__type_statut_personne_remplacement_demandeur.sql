-- Active: 1783014215025@@127.0.0.1@5432@gestionpatrimoine@public
-- ============================================================================
-- MIGRATION V11 : Types & statuts de personne + remplacement de la table demandeur
-- ----------------------------------------------------------------------------
-- 1) Nouvelles tables référentielles : type_personne, statut_personne
-- 2) Colonnes id_type_personne / id_statut_personne sur personne
-- 3) dossier_demandeur, document_demandeur, descente_terrain :
--    id_demandeur  ->  id_personne (FK vers personne)
-- 4) Suppression de la table demandeur
-- ============================================================================

-- ── 1) Tables référentielles ──────────────────────────────────────────────

CREATE TABLE type_personne(
   id_type_personne INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   code VARCHAR(50) NOT NULL UNIQUE,
   libelle VARCHAR(100) NOT NULL
);



-- ── 2) Seed des référentiels ──────────────────────────────────────────────

INSERT INTO type_personne(code, libelle) VALUES
  ('DEMANDEUR',    'Demandeur'),
  ('NOTIFIE',      'Personne notifiée'),
  ('AVERTI',       'Personne avertie'),
  ('OCCUPANT',     'Information occupant'),
  ('PROPRIETAIRE', 'Propriétaire'),
  ('MANDATAIRE',   'Mandataire');



-- ── 3) Personne : rattachement type / statut ──────────────────────────────

ALTER TABLE personne ADD COLUMN id_type_personne INT REFERENCES type_personne(id_type_personne);
-- Statut par défaut : Actif



-- Repli du type depuis l'ancien champ libre "role"
UPDATE personne p
SET id_type_personne = t.id_type_personne
FROM type_personne t
WHERE p.id_type_personne IS NULL
  AND t.code = CASE
    WHEN LOWER(p.role) LIKE '%notif%'  THEN 'NOTIFIE'
    WHEN LOWER(p.role) LIKE '%avert%'  THEN 'AVERTI'
    WHEN LOWER(p.role) LIKE '%occup%'  THEN 'OCCUPANT'
    WHEN LOWER(p.role) LIKE '%propri%' THEN 'PROPRIETAIRE'
    WHEN LOWER(p.role) LIKE '%mandat%' THEN 'MANDATAIRE'
    ELSE 'DEMANDEUR'
  END;

-- ── 4) dossier_demandeur : id_demandeur -> id_personne ────────────────────

ALTER TABLE dossier_demandeur DROP CONSTRAINT IF EXISTS dossier_demandeur_id_demandeur_fkey;
ALTER TABLE dossier_demandeur RENAME COLUMN id_demandeur TO id_personne;
ALTER TABLE dossier_demandeur ADD CONSTRAINT fk_dossier_demandeur_personne
  FOREIGN KEY(id_personne) REFERENCES personne(id_personne);

-- ── 5) document_demandeur : id_demandeur -> id_personne ───────────────────

ALTER TABLE document_demandeur DROP CONSTRAINT IF EXISTS document_demandeur_id_demandeur_fkey;
ALTER TABLE document_demandeur RENAME COLUMN id_demandeur TO id_personne;
ALTER TABLE document_demandeur ADD CONSTRAINT fk_document_demandeur_personne
  FOREIGN KEY(id_personne) REFERENCES personne(id_personne);

-- ── 6) descente_terrain : id_demandeur -> id_personne ─────────────────────

ALTER TABLE descente_terrain DROP CONSTRAINT IF EXISTS descente_terrain_id_demandeur_fkey;
ALTER TABLE descente_terrain RENAME COLUMN id_demandeur TO id_personne;
ALTER TABLE descente_terrain ADD CONSTRAINT fk_descente_terrain_personne
  FOREIGN KEY(id_personne) REFERENCES personne(id_personne);

-- ── 7) Suppression de la table demandeur ──────────────────────────────────

DROP TABLE demandeur;

-- ============================================================================
-- FIN DE LA MIGRATION V11
-- ============================================================================