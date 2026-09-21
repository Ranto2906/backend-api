-- Ajout de la valeur 'Import Excel' au CHECK constraint de la colonne source
-- dans la table geometrie, nécessaire pour l'import Excel des dossiers patrimoine.

ALTER TABLE geometrie DROP CONSTRAINT IF EXISTS geometrie_source_check;
ALTER TABLE geometrie ADD CONSTRAINT geometrie_source_check
CHECK(source IN('Tracé manuel','Import GPS','Calculé automatiquement','Point carte','Recherche de lieu','Import Excel'));
