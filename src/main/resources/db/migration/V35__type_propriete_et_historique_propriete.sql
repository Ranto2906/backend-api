-- ============================================================================
-- MIGRATION V35 : Type de propriété + historique des propriétés
-- ----------------------------------------------------------------------------
-- 1) Table type_propriete (référentiel)
-- 2) Colonne id_type_propriete nullable d'abord, puis backfill, puis NOT NULL
-- 3) Historique = table audit existante (entite_type = 'propriete')
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Référentiel type_propriete
-- ---------------------------------------------------------------------------
CREATE TABLE type_propriete (
   id_type_propriete INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   libelle           VARCHAR(80)  NOT NULL,
   UNIQUE(libelle)
);

-- Valeurs demandées pour la saisie des propriétés.
INSERT INTO type_propriete (libelle)
VALUES
  ('Indéterminé'),
  ('Parcelle cadastrale'),
  ('Titre borné');

-- ---------------------------------------------------------------------------
-- 2. Clé étrangère sur propriete (nullable d'abord, puis NOT NULL)
-- ---------------------------------------------------------------------------
ALTER TABLE propriete
  ADD COLUMN id_type_propriete INTEGER
    REFERENCES type_propriete(id_type_propriete);

CREATE INDEX IF NOT EXISTS idx_propriete_type ON propriete(id_type_propriete);

-- Backfill déterministe :
--   - utiliser 'Parcelle cadastrale' quand la propriété a des parcelles
--     rattachées (titre_foncier.parcelle existe)
--   - sinon utiliser 'Titre borné'
--   - si aucun type ne correspond, on retombe sur 'Indéterminé'
WITH premierType AS (
    SELECT id_type_propriete, libelle
    FROM type_propriete
    WHERE libelle = 'Parcelle cadastrale'
    LIMIT 1
),
secondType AS (
    SELECT id_type_propriete, libelle
    FROM type_propriete
    WHERE libelle = 'Titre borné'
    LIMIT 1
),
fallbackType AS (
    SELECT id_type_propriete, libelle
    FROM type_propriete
    WHERE libelle = 'Indéterminé'
    LIMIT 1
)
UPDATE propriete p
SET id_type_propriete = coalesce(
    (SELECT pt.id_type_propriete FROM premierType pt),
    (SELECT st.id_type_propriete FROM secondType st),
    (SELECT ft.id_type_propriete FROM fallbackType ft)
);

-- Une fois le backfill fait, on rend la colonne obligatoire.
ALTER TABLE propriete
  ALTER COLUMN id_type_propriete SET NOT NULL;

-- ---------------------------------------------------------------------------
-- 3. Historique
-- ---------------------------------------------------------------------------
-- Aucune table supplémentaire : les modifications de propriété sont tracées
-- dans la table audit existante (entite_type = 'propriete'), comme les
-- dossiers, les prix au m², les notifications, etc.
-- L'interface web affiche cet historique depuis /api/proprietes/{id}/historique
