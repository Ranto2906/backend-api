-- Ajout de la valeur 'Saisie coordonnées' au CHECK constraint de la colonne
-- source dans la table geometrie, nécessaire au formulaire « Saisir des
-- coordonnées » de la carte interactive (module 4.6.1).

ALTER TABLE geometrie DROP CONSTRAINT IF EXISTS geometrie_source_check;
ALTER TABLE geometrie ADD CONSTRAINT geometrie_source_check
CHECK(source IN('Tracé manuel','Import GPS','Calculé automatiquement','Point carte','Recherche de lieu','Import Excel','Saisie coordonnées'));
