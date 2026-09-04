-- ============================================================================
-- MIGRATION V15 : Référentiels des signalements
-- ----------------------------------------------------------------------------
-- Seed des tables type_signalement et statut_signalement (vides à ce jour).
-- Version idempotente : peut être ré-exécutée sans erreur.
-- ============================================================================

-- ── TYPES DE SIGNALEMENT ────────────────────────────────────────────────────
INSERT INTO type_signalement (code, libelle, couleur)
SELECT 'OCCUPATION',   'Occupation',   '#e67e22'
WHERE NOT EXISTS (SELECT 1 FROM type_signalement WHERE code = 'OCCUPATION');

INSERT INTO type_signalement (code, libelle, couleur)
SELECT 'CONSTRUCTION', 'Construction', '#e74c3c'
WHERE NOT EXISTS (SELECT 1 FROM type_signalement WHERE code = 'CONSTRUCTION');

INSERT INTO type_signalement (code, libelle, couleur)
SELECT 'AUTRE',        'Autre constat', '#3498db'
WHERE NOT EXISTS (SELECT 1 FROM type_signalement WHERE code = 'AUTRE');

-- ── STATUTS DE SIGNALEMENT ─────────────────────────────────────────────────
INSERT INTO statut_signalement (code, libelle, couleur_hex, est_final, ordre)
SELECT 'NOUVEAU',   'Nouveau',    '#e67e22', FALSE, 1
WHERE NOT EXISTS (SELECT 1 FROM statut_signalement WHERE code = 'NOUVEAU');

INSERT INTO statut_signalement (code, libelle, couleur_hex, est_final, ordre)
SELECT 'EN_ATTENTE', 'En attente', '#8a8578', FALSE, 2
WHERE NOT EXISTS (SELECT 1 FROM statut_signalement WHERE code = 'EN_ATTENTE');

INSERT INTO statut_signalement (code, libelle, couleur_hex, est_final, ordre)
SELECT 'EN_COURS',  'En cours',   '#3498db', FALSE, 3
WHERE NOT EXISTS (SELECT 1 FROM statut_signalement WHERE code = 'EN_COURS');

INSERT INTO statut_signalement (code, libelle, couleur_hex, est_final, ordre)
SELECT 'TRAITE',    'Traité',     '#27ae60', TRUE, 4
WHERE NOT EXISTS (SELECT 1 FROM statut_signalement WHERE code = 'TRAITE');

INSERT INTO statut_signalement (code, libelle, couleur_hex, est_final, ordre)
SELECT 'REJETE',    'Rejeté',     '#e74c3c', TRUE, 5
WHERE NOT EXISTS (SELECT 1 FROM statut_signalement WHERE code = 'REJETE');

INSERT INTO statut_signalement (code, libelle, couleur_hex, est_final, ordre)
SELECT 'TRANSFORME', 'Transformé', '#9b59b6', TRUE, 6
WHERE NOT EXISTS (SELECT 1 FROM statut_signalement WHERE code = 'TRANSFORME');

-- ============================================================================
-- FIN DE LA MIGRATION V15
-- ============================================================================