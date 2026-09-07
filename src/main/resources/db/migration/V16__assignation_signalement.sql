-- ============================================================================
-- MIGRATION V16 : Assignation signalement <-> notification / avertissement
-- ----------------------------------------------------------------------------
-- 1. Crée la table assignation (lien N..N entre un signalement et une
--    notification_occupation OU un avertissement).
-- 2. Reporte les liens existants depuis signalement.id_notification /
--    id_avertissement vers assignation.
-- 3. Supprime les colonnes id_notification / id_avertissement de signalement.
-- 4. Ajoute 'assignation' aux types d'entité acceptés par la table audit.
-- ============================================================================

CREATE TABLE IF NOT EXISTS assignation(
   id_assignation BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   id_signalement UUID NOT NULL REFERENCES signalement(id_signalement) ON DELETE CASCADE,
   id_notification UUID REFERENCES notification_occupation(id_notification) ON DELETE CASCADE,
   id_avertissement UUID REFERENCES avertissement(id_avertissement) ON DELETE CASCADE,
   date_assignation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   id_utilisateur INT REFERENCES utilisateur(id_utilisateur),
   CONSTRAINT assignation_cible_check CHECK(
       (id_notification IS NOT NULL AND id_avertissement IS NULL) OR
       (id_notification IS NULL     AND id_avertissement IS NOT NULL))
);

-- Une assignation unique par (signalement, notification) et (signalement, avertissement)
CREATE UNIQUE INDEX IF NOT EXISTS uq_assignation_signalement_notification
    ON assignation(id_signalement, id_notification) WHERE id_notification IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_assignation_signalement_avertissement
    ON assignation(id_signalement, id_avertissement) WHERE id_avertissement IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_assignation_notification ON assignation(id_notification);
CREATE INDEX IF NOT EXISTS idx_assignation_avertissement ON assignation(id_avertissement);

-- ── Report des liens existants (avant suppression des colonnes) ──
INSERT INTO assignation (id_signalement, id_notification, id_utilisateur)
SELECT id_signalement, id_notification, id_utilisateur_creation
FROM signalement
WHERE id_notification IS NOT NULL;

INSERT INTO assignation (id_signalement, id_avertissement, id_utilisateur)
SELECT id_signalement, id_avertissement, id_utilisateur_creation
FROM signalement
WHERE id_avertissement IS NOT NULL;

-- ── Suppression des colonnes devenues redondantes ──
ALTER TABLE signalement DROP COLUMN IF EXISTS id_notification;
ALTER TABLE signalement DROP COLUMN IF EXISTS id_avertissement;

-- ── Audit : accepter 'assignation' ──
ALTER TABLE audit DROP CONSTRAINT IF EXISTS audit_entite_type_check;

ALTER TABLE audit
ADD CONSTRAINT audit_entite_type_check
CHECK(entite_type IN('dossier','notification_occupation','avertissement',
                       'parcelle','titre_foncier','propriete','utilisateur',
                       'geometrie','signalement','descente_terrain','personne','ville',
                       'prix_m2','role','suivi_notification','assignation'));

-- ============================================================================
-- FIN DE LA MIGRATION V16
-- ============================================================================
