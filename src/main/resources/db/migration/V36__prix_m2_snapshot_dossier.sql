-- ============================================================================
-- MIGRATION V36 : Prix au m² figé (snapshot) sur le dossier
-- Le prix au m² est enregistré sur le dossier au moment de sa création :
-- modifier un prix du référentiel ne modifie JAMAIS les anciens dossiers,
-- leurs prix unitaires ni leurs montants totaux.
-- Seules les nouvelles demandes créées après la modification prennent le
-- nouveau prix en vigueur.
-- ============================================================================

-- ── 1. Colonnes de snapshot ──
ALTER TABLE dossier ADD COLUMN IF NOT EXISTS prix_m2_ttc DECIMAL(14,2);
ALTER TABLE dossier ADD COLUMN IF NOT EXISTS prix_m2_ht  DECIMAL(14,2);

-- ── 2. Reconstitution rétroactive : prix en vigueur à la date du dossier ──
-- (dernier prix de la ville dont date_maj <= date de la demande/création)
UPDATE dossier d
SET prix_m2_ttc = (
        SELECT p.prix_ttc FROM prix_m2 p
        WHERE p.id_ville = d.id_ville
          AND p.date_maj <= COALESCE(d.date_demande, d.date_creation::date)
        ORDER BY p.date_maj DESC, p.id_prix DESC
        LIMIT 1),
    prix_m2_ht = (
        SELECT p.prix_ht FROM prix_m2 p
        WHERE p.id_ville = d.id_ville
          AND p.date_maj <= COALESCE(d.date_demande, d.date_creation::date)
        ORDER BY p.date_maj DESC, p.id_prix DESC
        LIMIT 1)
WHERE d.prix_m2_ttc IS NULL
  AND EXISTS (SELECT 1 FROM prix_m2 p
              WHERE p.id_ville = d.id_ville
                AND p.date_maj <= COALESCE(d.date_demande, d.date_creation::date));

-- ── 3. Dossiers sans prix historique : dernier prix connu de la ville ──
UPDATE dossier d
SET prix_m2_ttc = (SELECT p.prix_ttc FROM prix_m2 p
                   WHERE p.id_ville = d.id_ville
                   ORDER BY p.date_maj DESC, p.id_prix DESC LIMIT 1),
    prix_m2_ht  = (SELECT p.prix_ht  FROM prix_m2 p
                   WHERE p.id_ville = d.id_ville
                   ORDER BY p.date_maj DESC, p.id_prix DESC LIMIT 1)
WHERE d.prix_m2_ttc IS NULL
  AND EXISTS (SELECT 1 FROM prix_m2 p WHERE p.id_ville = d.id_ville);

-- Les montants déjà enregistrés (montant_total) ne sont PAS modifiés.

-- ============================================================================
-- FIN DE LA MIGRATION V36
-- ============================================================================
