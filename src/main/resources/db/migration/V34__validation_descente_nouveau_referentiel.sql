-- ══════════════════════════════════════════════════════════════
-- V34 — Nouveau referentiel de validation des descentes terrain
-- ══════════════════════════════════════════════════════════════
--
-- Remplace « En attente / Valide / Rejete / Complement demande »
-- par 2 statuts : « Bon pour la suite » et « En instance ».
-- Decision metier :
--   - toutes les descentes existantes → « Bon pour la suite » ;
--   - une NOUVELLE descente demarre « En instance » (dans le code).

-- 1) Retrait de l'ancienne contrainte AVANT la mise a jour des
--    donnees (sinon les nouvelles valeurs la violent)
ALTER TABLE descente_terrain DROP CONSTRAINT IF EXISTS descente_terrain_validation_check;

-- 2) Reaffectation des donnees existantes
UPDATE descente_terrain
SET validation = 'Bon pour la suite'
WHERE validation IS NULL OR validation NOT IN ('Bon pour la suite', 'En instance');

-- 3) La nouvelle contrainte
ALTER TABLE descente_terrain
   ADD CONSTRAINT descente_terrain_validation_check
   CHECK (validation IN ('Bon pour la suite', 'En instance'));
