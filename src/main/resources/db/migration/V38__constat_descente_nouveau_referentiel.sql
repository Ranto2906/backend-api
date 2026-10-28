-- ══════════════════════════════════════════════════════════════
-- V38 — Nouveau referentiel de constat des descentes terrain
-- ══════════════════════════════════════════════════════════════
--
-- Remplace « Conforme / Non conforme / En attente / Occupation illicite /
-- Construction illegale » par 3 constats : Faisable / Non faisable /
-- Sous réserve.
--
-- Reaffectation des donnees existantes :
--   - Conforme, En attente        -> Faisable
--   - Non conforme, Occupation
--     illicite, Construction
--     illegale                    -> Non faisable
-- Une NOUVELLE descente demarre « Faisable » (dans le code).

-- 1) Retrait de l'ancienne contrainte AVANT la mise a jour des
--    donnees (sinon les nouvelles valeurs la violent)
ALTER TABLE descente_terrain DROP CONSTRAINT IF EXISTS descente_terrain_statut_constat_check;

-- 2) Reaffectation des donnees existantes
UPDATE descente_terrain
SET statut_constat = 'Non faisable'
WHERE statut_constat IN ('Non conforme', 'Occupation illicite', 'Construction illegale');

UPDATE descente_terrain
SET statut_constat = 'Faisable'
WHERE statut_constat IS NULL OR statut_constat NOT IN ('Faisable', 'Non faisable', 'Sous reserve', 'Sous réserve');

-- 3) La nouvelle contrainte + valeur par defaut
ALTER TABLE descente_terrain
   ALTER COLUMN statut_constat SET DEFAULT 'Faisable';

ALTER TABLE descente_terrain
   ADD CONSTRAINT descente_terrain_statut_constat_check
   CHECK (statut_constat IN ('Faisable', 'Non faisable', 'Sous réserve'));
