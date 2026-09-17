-- ============================================================================
-- V19 : suppression des colonnes valeur_demande_ttc / valeur_demande_ht
-- de la table dossier_parcelle.
-- La valeur de la demande est désormais calculée à la volée :
--   superficie_m2 × prix au m² (TTC/HT) de la ville du dossier.
-- ============================================================================

ALTER TABLE dossier_parcelle DROP COLUMN IF EXISTS valeur_demande_ttc;
ALTER TABLE dossier_parcelle DROP COLUMN IF EXISTS valeur_demande_ht;
