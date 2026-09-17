-- Ajout du type 'propriete' au CHECK constraint de la table geometrie
-- pour permettre de rattacher une géométrie à une propriété (point d'ancrage).

ALTER TABLE dossier_parcelle DROP COLUMN coordonnee_x_;
ALTER TABLE dossier_parcelle DROP COLUMN coordonnee_y_;

ALTER TABLE geometrie DROP CONSTRAINT IF EXISTS geometrie_entite_type_check;
ALTER TABLE geometrie ADD CONSTRAINT geometrie_entite_type_check
CHECK(entite_type IN('parcelle','dossier_parcelle','signalement','descente_terrain','propriete'));
