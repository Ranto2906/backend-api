-- ============================================================================
-- MIGRATION V13 : Observation sur les photos
-- ----------------------------------------------------------------------------
-- Ajout d'un champ libre « observation » (légende / note de terrain) à chaque
-- photo, rempli lors de l'ajout manuel d'une photo depuis le formulaire.
-- ============================================================================

ALTER TABLE photo ADD COLUMN observation TEXT;

-- ============================================================================
-- FIN DE LA MIGRATION V13
-- ============================================================================
