-- ============================================================================
-- MIGRATION V2 : Seed data — Rôles, Modules, Entités, Actions, Permissions
--                et compte administrateur par défaut (RG-05)
-- Version idempotente : peut être ré-exécutée sans erreur
-- ============================================================================

-- ── ROLES (RG-05) ──
INSERT INTO role (nom_role, description)
SELECT 'Administrateur', 'Accès complet : création/modification/suppression sur toutes les entités, gestion des utilisateurs et rôles, réinitialisation de la base, export.'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE nom_role = 'Administrateur');

INSERT INTO role (nom_role, description)
SELECT 'Gestionnaire foncier', 'Consultation et saisie des demandes, parcelles, titres, notifications ; tracé sur la carte ; saisie des coordonnées.'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE nom_role = 'Gestionnaire foncier');

INSERT INTO role (nom_role, description)
SELECT 'Agent terrain', 'Application mobile : consultation des notifications et avertissements, saisie des constats et photos, mode hors ligne, synchronisation en différé.'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE nom_role = 'Agent terrain');

INSERT INTO role (nom_role, description)
SELECT 'Observateur', 'Consultation seule — accès en lecture sur les données sans possibilité de modification.'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE nom_role = 'Observateur');

-- ── MODULES ──
INSERT INTO module_ (code_module, libelle)
SELECT 'AUTH', 'Authentification et utilisateurs'
WHERE NOT EXISTS (SELECT 1 FROM module_ WHERE code_module = 'AUTH');

INSERT INTO module_ (code_module, libelle)
SELECT 'DEMANDE', 'Gestion des demandes (dossiers)'
WHERE NOT EXISTS (SELECT 1 FROM module_ WHERE code_module = 'DEMANDE');

INSERT INTO module_ (code_module, libelle)
SELECT 'PATRIMOINE', 'Patrimoine foncier'
WHERE NOT EXISTS (SELECT 1 FROM module_ WHERE code_module = 'PATRIMOINE');

INSERT INTO module_ (code_module, libelle)
SELECT 'SUIVI', 'Suivi terrain'
WHERE NOT EXISTS (SELECT 1 FROM module_ WHERE code_module = 'SUIVI');

INSERT INTO module_ (code_module, libelle)
SELECT 'CARTE', 'Carte interactive et géométries'
WHERE NOT EXISTS (SELECT 1 FROM module_ WHERE code_module = 'CARTE');

INSERT INTO module_ (code_module, libelle)
SELECT 'SIGNALEMENT', 'Signalements'
WHERE NOT EXISTS (SELECT 1 FROM module_ WHERE code_module = 'SIGNALEMENT');

INSERT INTO module_ (code_module, libelle)
SELECT 'FINANCE', 'Synthèse financière'
WHERE NOT EXISTS (SELECT 1 FROM module_ WHERE code_module = 'FINANCE');

INSERT INTO module_ (code_module, libelle)
SELECT 'DOCUMENT', 'Gestion documentaire'
WHERE NOT EXISTS (SELECT 1 FROM module_ WHERE code_module = 'DOCUMENT');

INSERT INTO module_ (code_module, libelle)
SELECT 'ADMIN', 'Administration'
WHERE NOT EXISTS (SELECT 1 FROM module_ WHERE code_module = 'ADMIN');

-- ── ENTITÉS ──
-- MODULE 0 : AUTH
INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'UTILISATEUR', 'Utilisateur', (SELECT id_module FROM module_ WHERE code_module = 'AUTH')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'UTILISATEUR');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'ROLE', 'Rôle', (SELECT id_module FROM module_ WHERE code_module = 'AUTH')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'ROLE');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'PERMISSION', 'Permission', (SELECT id_module FROM module_ WHERE code_module = 'AUTH')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'PERMISSION');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'JOURNAL', 'Journal connexion', (SELECT id_module FROM module_ WHERE code_module = 'AUTH')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'JOURNAL');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'SESSION', 'Session', (SELECT id_module FROM module_ WHERE code_module = 'AUTH')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'SESSION');

-- MODULE 1 : DEMANDE
INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'DOSSIER', 'Dossier (demande)', (SELECT id_module FROM module_ WHERE code_module = 'DEMANDE')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'DOSSIER');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'DEMANDEUR', 'Demandeur', (SELECT id_module FROM module_ WHERE code_module = 'DEMANDE')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'DEMANDEUR');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'ETAPE', 'Étape', (SELECT id_module FROM module_ WHERE code_module = 'DEMANDE')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'ETAPE');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'SUIVI_DOSSIER', 'Suivi dossier', (SELECT id_module FROM module_ WHERE code_module = 'DEMANDE')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'SUIVI_DOSSIER');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'DOSSIER_PARCELLE', 'Parcelle de dossier', (SELECT id_module FROM module_ WHERE code_module = 'DEMANDE')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'DOSSIER_PARCELLE');

-- MODULE 2 : PATRIMOINE
INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'VILLE', 'Ville', (SELECT id_module FROM module_ WHERE code_module = 'PATRIMOINE')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'VILLE');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'PRIX_M2', 'Prix au m²', (SELECT id_module FROM module_ WHERE code_module = 'PATRIMOINE')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'PRIX_M2');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'PROPRIETE', 'Propriété', (SELECT id_module FROM module_ WHERE code_module = 'PATRIMOINE')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'PROPRIETE');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'TITRE_FONCIER', 'Titre foncier', (SELECT id_module FROM module_ WHERE code_module = 'PATRIMOINE')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'TITRE_FONCIER');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'PARCELLE', 'Parcelle', (SELECT id_module FROM module_ WHERE code_module = 'PATRIMOINE')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'PARCELLE');

-- MODULE 3 : SUIVI
INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'NOTIFICATION', 'Notification occupation', (SELECT id_module FROM module_ WHERE code_module = 'SUIVI')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'NOTIFICATION');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'AVERTISSEMENT', 'Avertissement', (SELECT id_module FROM module_ WHERE code_module = 'SUIVI')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'AVERTISSEMENT');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'PERSONNE', 'Personne', (SELECT id_module FROM module_ WHERE code_module = 'SUIVI')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'PERSONNE');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'SUIVI_NOTIFICATION', 'Suivi notification', (SELECT id_module FROM module_ WHERE code_module = 'SUIVI')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'SUIVI_NOTIFICATION');

-- MODULE 4 : CARTE
INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'GEOMETRIE', 'Géométrie', (SELECT id_module FROM module_ WHERE code_module = 'CARTE')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'GEOMETRIE');

-- MODULE 5 : SIGNALEMENT
INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'SIGNALEMENT', 'Signalement', (SELECT id_module FROM module_ WHERE code_module = 'SIGNALEMENT')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'SIGNALEMENT');

-- MODULE 6 : FINANCE
INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'VUE_RECOUVREMENT', 'Vue récouvrement', (SELECT id_module FROM module_ WHERE code_module = 'FINANCE')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'VUE_RECOUVREMENT');

-- MODULE 7 : DOCUMENT
INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'DOCUMENT_DOSSIER', 'Document dossier', (SELECT id_module FROM module_ WHERE code_module = 'DOCUMENT')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'DOCUMENT_DOSSIER');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'DOCUMENT_DEMANDEUR', 'Document demandeur', (SELECT id_module FROM module_ WHERE code_module = 'DOCUMENT')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'DOCUMENT_DEMANDEUR');

-- MODULE 8 : ADMIN
INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'AUDIT', 'Audit', (SELECT id_module FROM module_ WHERE code_module = 'ADMIN')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'AUDIT');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'ALERTE', 'Alerte', (SELECT id_module FROM module_ WHERE code_module = 'ADMIN')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'ALERTE');

INSERT INTO entite (code_entite, libelle, id_module)
SELECT 'DEScente_TERRAIN', 'Descente terrain', (SELECT id_module FROM module_ WHERE code_module = 'ADMIN')
WHERE NOT EXISTS (SELECT 1 FROM entite WHERE code_entite = 'DEScente_TERRAIN');

-- ── ACTIONS ──
INSERT INTO action (code_action)
SELECT 'CREATE' WHERE NOT EXISTS (SELECT 1 FROM action WHERE code_action = 'CREATE');

INSERT INTO action (code_action)
SELECT 'READ' WHERE NOT EXISTS (SELECT 1 FROM action WHERE code_action = 'READ');

INSERT INTO action (code_action)
SELECT 'UPDATE' WHERE NOT EXISTS (SELECT 1 FROM action WHERE code_action = 'UPDATE');

INSERT INTO action (code_action)
SELECT 'DELETE' WHERE NOT EXISTS (SELECT 1 FROM action WHERE code_action = 'DELETE');

INSERT INTO action (code_action)
SELECT 'EXPORT' WHERE NOT EXISTS (SELECT 1 FROM action WHERE code_action = 'EXPORT');

INSERT INTO action (code_action)
SELECT 'VALIDATE' WHERE NOT EXISTS (SELECT 1 FROM action WHERE code_action = 'VALIDATE');

-- ── PERMISSIONS �──
-- Génération automatique : toutes les combinaisons (entité × action) pour les modules essentiels

-- AUTH - CRUD complet pour l'admin
INSERT INTO permission (id_action, id_entite)
SELECT a.id_action, e.id_entite
FROM action a
CROSS JOIN entite e
WHERE a.code_action IN ('CREATE', 'READ', 'UPDATE', 'DELETE')
  AND e.code_entite IN ('UTILISATEUR', 'ROLE', 'PERMISSION', 'JOURNAL', 'SESSION')
  AND NOT EXISTS (
    SELECT 1 FROM permission p
    WHERE p.id_action = a.id_action AND p.id_entite = e.id_entite
  );

-- DEMANDE - CRUD pour gestionnaire
INSERT INTO permission (id_action, id_entite)
SELECT a.id_action, e.id_entite
FROM action a
CROSS JOIN entite e
WHERE a.code_action IN ('CREATE', 'READ', 'UPDATE', 'DELETE')
  AND e.code_entite IN ('DOSSIER', 'DEMANDEUR', 'ETAPE', 'SUIVI_DOSSIER', 'DOSSIER_PARCELLE')
  AND NOT EXISTS (
    SELECT 1 FROM permission p
    WHERE p.id_action = a.id_action AND p.id_entite = e.id_entite
  );

-- PATRIMOINE - CRUD pour gestionnaire
INSERT INTO permission (id_action, id_entite)
SELECT a.id_action, e.id_entite
FROM action a
CROSS JOIN entite e
WHERE a.code_action IN ('CREATE', 'READ', 'UPDATE', 'DELETE')
  AND e.code_entite IN ('VILLE', 'PRIX_M2', 'PROPRIETE', 'TITRE_FONCIER', 'PARCELLE')
  AND NOT EXISTS (
    SELECT 1 FROM permission p
    WHERE p.id_action = a.id_action AND p.id_entite = e.id_entite
  );

-- SUIVI - CRUD pour agent terrain + gestionnaire
INSERT INTO permission (id_action, id_entite)
SELECT a.id_action, e.id_entite
FROM action a
CROSS JOIN entite e
WHERE a.code_action IN ('CREATE', 'READ', 'UPDATE', 'DELETE')
  AND e.code_entite IN ('NOTIFICATION', 'AVERTISSEMENT', 'PERSONNE', 'SUIVI_NOTIFICATION')
  AND NOT EXISTS (
    SELECT 1 FROM permission p
    WHERE p.id_action = a.id_action AND p.id_entite = e.id_entite
  );

-- CARTE - CRUD
INSERT INTO permission (id_action, id_entite)
SELECT a.id_action, e.id_entite
FROM action a
CROSS JOIN entite e
WHERE a.code_action IN ('CREATE', 'READ', 'UPDATE', 'DELETE')
  AND e.code_entite = 'GEOMETRIE'
  AND NOT EXISTS (
    SELECT 1 FROM permission p
    WHERE p.id_action = a.id_action AND p.id_entite = e.id_entite
  );

-- SIGNALEMENT - CRUD
INSERT INTO permission (id_action, id_entite)
SELECT a.id_action, e.id_entite
FROM action a
CROSS JOIN entite e
WHERE a.code_action IN ('CREATE', 'READ', 'UPDATE', 'DELETE')
  AND e.code_entite = 'SIGNALEMENT'
  AND NOT EXISTS (
    SELECT 1 FROM permission p
    WHERE p.id_action = a.id_action AND p.id_entite = e.id_entite
  );

-- FINANCE - READ + EXPORT
INSERT INTO permission (id_action, id_entite)
SELECT a.id_action, e.id_entite
FROM action a
CROSS JOIN entite e
WHERE a.code_action IN ('READ', 'EXPORT')
  AND e.code_entite = 'VUE_RECOUVREMENT'
  AND NOT EXISTS (
    SELECT 1 FROM permission p
    WHERE p.id_action = a.id_action AND p.id_entite = e.id_entite
  );

-- DOCUMENT - CRUD
INSERT INTO permission (id_action, id_entite)
SELECT a.id_action, e.id_entite
FROM action a
CROSS JOIN entite e
WHERE a.code_action IN ('CREATE', 'READ', 'UPDATE', 'DELETE')
  AND e.code_entite IN ('DOCUMENT_DOSSIER', 'DOCUMENT_DEMANDEUR')
  AND NOT EXISTS (
    SELECT 1 FROM permission p
    WHERE p.id_action = a.id_action AND p.id_entite = e.id_entite
  );

-- ADMIN - READ + VALIDATE
INSERT INTO permission (id_action, id_entite)
SELECT a.id_action, e.id_entite
FROM action a
CROSS JOIN entite e
WHERE a.code_action IN ('READ', 'VALIDATE')
  AND e.code_entite IN ('AUDIT', 'ALERTE', 'DEScente_TERRAIN')
  AND NOT EXISTS (
    SELECT 1 FROM permission p
    WHERE p.id_action = a.id_action AND p.id_entite = e.id_entite
  );

-- ── ATTRIBUTION DES PERMISSIONS AUX RÔLES ──
-- Administrateur : TOUTES les permissions
INSERT INTO role_permission (id_role, id_permission)
SELECT r.id_role, p.id_permission
FROM role r
CROSS JOIN permission p
WHERE r.nom_role = 'Administrateur'
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.id_role = r.id_role AND rp.id_permission = p.id_permission
  );

-- Gestionnaire foncier : DEMANDE + PATRIMOINE + SUIVI + CARTE + FINANCE + DOCUMENT
INSERT INTO role_permission (id_role, id_permission)
SELECT r.id_role, p.id_permission
FROM role r
CROSS JOIN permission p
JOIN entite e ON p.id_entite = e.id_entite
JOIN module_ m ON e.id_module = m.id_module
WHERE r.nom_role = 'Gestionnaire foncier'
  AND m.code_module IN ('DEMANDE', 'PATRIMOINE', 'SUIVI', 'CARTE', 'FINANCE', 'DOCUMENT')
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.id_role = r.id_role AND rp.id_permission = p.id_permission
  );

-- Agent terrain : SUIVI (READ/UPDATE) + SIGNALEMENT (READ/CREATE)
INSERT INTO role_permission (id_role, id_permission)
SELECT r.id_role, p.id_permission
FROM role r
CROSS JOIN permission p
JOIN entite e ON p.id_entite = e.id_entite
JOIN module_ m ON e.id_module = m.id_module
JOIN action a ON p.id_action = a.id_action
WHERE r.nom_role = 'Agent terrain'
  AND (
      (m.code_module = 'SUIVI' AND a.code_action IN ('READ', 'UPDATE'))
      OR (m.code_module = 'SIGNALEMENT' AND a.code_action IN ('READ', 'CREATE'))
  )
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.id_role = r.id_role AND rp.id_permission = p.id_permission
  );

-- Observateur : READ uniquement sur tout
INSERT INTO role_permission (id_role, id_permission)
SELECT r.id_role, p.id_permission
FROM role r
CROSS JOIN permission p
JOIN action a ON p.id_action = a.id_action
WHERE r.nom_role = 'Observateur'
  AND a.code_action = 'READ'
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.id_role = r.id_role AND rp.id_permission = p.id_permission
  );

-- ── COMPTE ADMIN PAR DÉFAUT (RG-05) ──
-- Mot de passe 'admin123' haché en BCrypt
INSERT INTO utilisateur (nom_utilisateur, nom_complet, email, mot_de_passe_hash, actif, statut_compte)
SELECT 'admin', 'Administrateur SEIMAD', 'admin@seimad.mg',
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       TRUE, 'actif'
WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE nom_utilisateur = 'admin');

-- Attribuer le rôle Administrateur au compte admin
INSERT INTO utilisateur_role (id_utilisateur, id_role)
SELECT u.id_utilisateur, r.id_role
FROM utilisateur u
CROSS JOIN role r
WHERE u.nom_utilisateur = 'admin'
  AND r.nom_role = 'Administrateur'
  AND NOT EXISTS (
    SELECT 1 FROM utilisateur_role ur
    WHERE ur.id_utilisateur = u.id_utilisateur AND ur.id_role = r.id_role
  );

-- ============================================================================
-- FIN DE LA MIGRATION V2
-- ============================================================================
