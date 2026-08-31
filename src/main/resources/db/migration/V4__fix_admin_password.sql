-- ============================================================================
-- MIGRATION V4 : Correction des mots de passe (hash BCrypt incorrect)
-- Tous les comptes utilisent désormais le mot de passe 'admin'
-- Déverrouille les comptes verrouillés et réinitialise les tentatives
-- ============================================================================

-- ── Hash BCrypt correct pour le mot de passe 'admin' (cost=10) ──
-- Généré via bcryptjs, compatible BCrypt Java (Spring Security BCryptPasswordEncoder)

-- Mettre à jour TOUS les utilisateurs avec le bon hash
UPDATE utilisateur
SET mot_de_passe_hash = '$2a$10$BuvLDkbLNOv0gtCWcnPUSeeQ1ik1RO9LP6II3oc7EOk2Tc7ZkBzvS';

-- ── Déverrouiller tous les comptes ──
UPDATE utilisateur
SET tentatives_echouees = 0,
    verrouille_jusqu_a = NULL,
    statut_compte = 'actif',
    actif = TRUE;

-- ============================================================================
-- FIN DE LA MIGRATION V4
-- ============================================================================
