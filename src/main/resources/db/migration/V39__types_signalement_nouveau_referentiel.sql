-- ══════════════════════════════════════════════════════════════
-- V39 — Nouveau referentiel des types de signalement
-- ══════════════════════════════════════════════════════════════
--
-- Remplace « Occupation / Construction / Autre constat » par
-- 3 types : Construction / Clôture / Terrain cultivé.
--
-- Les lignes sont RENOMMEES sur place (code + libellé + couleur) afin
-- que les signalements deja enregistrés (FK id_type_signalement)
-- suivent automatiquement le nouveau libellé, sans rupture de ref.
--
--   id 1  OCCUPATION   -> CLOTURE         « Clôture »         #e67e22
--   id 2  CONSTRUCTION -> CONSTRUCTION    « Construction »    #e74c3c (inchangé)
--   id 3  AUTRE        -> TERRAIN_CULTIVE « Terrain cultivé » #27ae60

-- 1) Occupation → Clôture
UPDATE type_signalement
SET code = 'CLOTURE', libelle = 'Clôture', couleur = '#e67e22'
WHERE code = 'OCCUPATION';

-- 2) Construction : libellé/couleur harmonisés (valeur conservée)
UPDATE type_signalement
SET libelle = 'Construction', couleur = '#e74c3c'
WHERE code = 'CONSTRUCTION';

-- 3) Autre constat → Terrain cultivé
UPDATE type_signalement
SET code = 'TERRAIN_CULTIVE', libelle = 'Terrain cultivé', couleur = '#27ae60'
WHERE code = 'AUTRE';

-- 4) Tout autre type éventuel est retiré : les signalements qui le
--    portent basculent d'abord sur « Construction » (FK NOT NULL),
--    puis la ligne est supprimée — le référentiel se réduit aux 3 types.
UPDATE signalement s
SET id_type_signalement = (SELECT id_type_signalement
                           FROM type_signalement
                           WHERE code = 'CONSTRUCTION')
WHERE s.id_type_signalement IN (SELECT id_type_signalement
                                FROM type_signalement
                                WHERE code NOT IN ('CONSTRUCTION', 'CLOTURE', 'TERRAIN_CULTIVE'));

DELETE FROM type_signalement
WHERE code NOT IN ('CONSTRUCTION', 'CLOTURE', 'TERRAIN_CULTIVE');
