-- Ajouter 'prix_m2' au CHECK constraint de la table audit
-- pour permettre l'audit des modifications sur les prix au m²

ALTER TABLE audit DROP CONSTRAINT IF EXISTS audit_entite_type_check;

ALTER TABLE audit
ADD CONSTRAINT audit_entite_type_check
CHECK(entite_type IN('dossier','notification_occupation','avertissement',
                       'parcelle','titre_foncier','propriete','utilisateur',
                       'geometrie','signalement','descente_terrain','personne','ville',
                       'prix_m2','role'));
