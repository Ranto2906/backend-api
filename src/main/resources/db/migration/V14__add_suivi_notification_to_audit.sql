-- ============================================================================
-- MIGRATION V14 : Audit des suivis de notification
-- ----------------------------------------------------------------------------
-- Ajoute 'suivi_notification' aux types d'entité acceptés par la table audit
-- afin de tracer la création, la modification et la suppression des suivis.
-- ============================================================================

ALTER TABLE audit DROP CONSTRAINT IF EXISTS audit_entite_type_check;

ALTER TABLE audit
ADD CONSTRAINT audit_entite_type_check
CHECK(entite_type IN('dossier','notification_occupation','avertissement',
                       'parcelle','titre_foncier','propriete','utilisateur',
                       'geometrie','signalement','descente_terrain','personne','ville',
                       'prix_m2','role','suivi_notification'));

-- ============================================================================
-- FIN DE LA MIGRATION V14
-- ============================================================================
