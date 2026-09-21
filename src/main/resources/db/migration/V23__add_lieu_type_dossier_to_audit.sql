-- ══════════════════════════════════════════════════════════════
-- V23 — Étendre la contrainte d'audit aux nouveaux référentiels
-- (lieu, type_dossier, dossier_fournie introduits par V21/V22)
-- ══════════════════════════════════════════════════════════════

ALTER TABLE audit DROP CONSTRAINT IF EXISTS audit_entite_type_check;

ALTER TABLE audit ADD CONSTRAINT audit_entite_type_check
CHECK(entite_type IN('dossier','notification_occupation','avertissement',
                     'parcelle','propriete','utilisateur','geometrie',
                     'signalement','descente_terrain','personne','ville',
                     'prix_m2','role','suivi_notification','assignation',
                     'lieu','type_dossier','dossier_fournie',
                     'etape','suivi_dossier'));
