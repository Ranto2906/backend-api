-- ============================================================================
-- MIGRATION V3 : Données seed complémentaires (idempotent)
-- ============================================================================

-- ── COMPTES DÉMO (tous avec mot de passe 'admin123') ──
INSERT INTO utilisateur (nom_utilisateur, nom_complet, email, mot_de_passe_hash, actif, statut_compte)
SELECT 'gestionnaire', 'Gestionnaire Foncier Test', 'gestionnaire@seimad.mg',
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       TRUE, 'actif'
WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE nom_utilisateur = 'gestionnaire');

INSERT INTO utilisateur_role (id_utilisateur, id_role)
SELECT u.id_utilisateur, r.id_role
FROM utilisateur u, role r
WHERE u.nom_utilisateur = 'gestionnaire' AND r.nom_role = 'Gestionnaire foncier'
  AND NOT EXISTS (
    SELECT 1 FROM utilisateur_role ur
    WHERE ur.id_utilisateur = u.id_utilisateur AND ur.id_role = r.id_role
  );

INSERT INTO utilisateur (nom_utilisateur, nom_complet, email, mot_de_passe_hash, actif, statut_compte)
SELECT 'agent_terrain', 'Agent Terrain Test', 'agent.terrain@seimad.mg',
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       TRUE, 'actif'
WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE nom_utilisateur = 'agent_terrain');

INSERT INTO utilisateur_role (id_utilisateur, id_role)
SELECT u.id_utilisateur, r.id_role
FROM utilisateur u, role r
WHERE u.nom_utilisateur = 'agent_terrain' AND r.nom_role = 'Agent terrain'
  AND NOT EXISTS (
    SELECT 1 FROM utilisateur_role ur
    WHERE ur.id_utilisateur = u.id_utilisateur AND ur.id_role = r.id_role
  );

INSERT INTO utilisateur (nom_utilisateur, nom_complet, email, mot_de_passe_hash, actif, statut_compte)
SELECT 'observateur', 'Observateur Test', 'observateur@seimad.mg',
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       TRUE, 'actif'
WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE nom_utilisateur = 'observateur');

INSERT INTO utilisateur_role (id_utilisateur, id_role)
SELECT u.id_utilisateur, r.id_role
FROM utilisateur u, role r
WHERE u.nom_utilisateur = 'observateur' AND r.nom_role = 'Observateur'
  AND NOT EXISTS (
    SELECT 1 FROM utilisateur_role ur
    WHERE ur.id_utilisateur = u.id_utilisateur AND ur.id_role = r.id_role
  );

-- ── STATUTS DE DOSSIER ──
INSERT INTO statut_dossier (code, libelle, couleur_hex, est_final, ordre)
SELECT 'En cours', 'En cours', '#f0ad4e', FALSE, 1
WHERE NOT EXISTS (SELECT 1 FROM statut_dossier WHERE code = 'En cours');
INSERT INTO statut_dossier (code, libelle, couleur_hex, est_final, ordre)
SELECT 'Valide', 'Validé', '#5cb85c', FALSE, 2
WHERE NOT EXISTS (SELECT 1 FROM statut_dossier WHERE code = 'Valide');
INSERT INTO statut_dossier (code, libelle, couleur_hex, est_final, ordre)
SELECT 'Rejete', 'Rejeté', '#d9534f', TRUE, 3
WHERE NOT EXISTS (SELECT 1 FROM statut_dossier WHERE code = 'Rejete');
INSERT INTO statut_dossier (code, libelle, couleur_hex, est_final, ordre)
SELECT 'Annule', 'Annulé', '#8a8578', TRUE, 4
WHERE NOT EXISTS (SELECT 1 FROM statut_dossier WHERE code = 'Annule');
INSERT INTO statut_dossier (code, libelle, couleur_hex, est_final, ordre)
SELECT 'Termine', 'Terminé', '#0275d8', TRUE, 5
WHERE NOT EXISTS (SELECT 1 FROM statut_dossier WHERE code = 'Termine');

-- ── ÉTAPES ──
INSERT INTO etape (code_etape, libelle, ordre, duree_previsionnelle)
SELECT 'DEPOT', 'Dépôt du dossier', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM etape WHERE code_etape = 'DEPOT');
INSERT INTO etape (code_etape, libelle, ordre, duree_previsionnelle)
SELECT 'INSTRUCTION', 'Instruction du dossier', 2, 15
WHERE NOT EXISTS (SELECT 1 FROM etape WHERE code_etape = 'INSTRUCTION');
INSERT INTO etape (code_etape, libelle, ordre, duree_previsionnelle)
SELECT 'ETUDE', 'Étude technique', 3, 30
WHERE NOT EXISTS (SELECT 1 FROM etape WHERE code_etape = 'ETUDE');
INSERT INTO etape (code_etape, libelle, ordre, duree_previsionnelle)
SELECT 'VISITA_TERRAIN', 'Visite terrain', 4, 10
WHERE NOT EXISTS (SELECT 1 FROM etape WHERE code_etape = 'VISITA_TERRAIN');
INSERT INTO etape (code_etape, libelle, ordre, duree_previsionnelle)
SELECT 'AVIS_TECHNIQUE', 'Avis technique', 5, 5
WHERE NOT EXISTS (SELECT 1 FROM etape WHERE code_etape = 'AVIS_TECHNIQUE');
INSERT INTO etape (code_etape, libelle, ordre, duree_previsionnelle)
SELECT 'DELIBERATION', 'Délibération', 6, 10
WHERE NOT EXISTS (SELECT 1 FROM etape WHERE code_etape = 'DELIBERATION');
INSERT INTO etape (code_etape, libelle, ordre, duree_previsionnelle)
SELECT 'SIGNATURE', 'Signature de la décision', 7, 5
WHERE NOT EXISTS (SELECT 1 FROM etape WHERE code_etape = 'SIGNATURE');
INSERT INTO etape (code_etape, libelle, ordre, duree_previsionnelle)
SELECT 'NOTIFICATION', 'Notification au demandeur', 8, 5
WHERE NOT EXISTS (SELECT 1 FROM etape WHERE code_etape = 'NOTIFICATION');
INSERT INTO etape (code_etape, libelle, ordre, duree_previsionnelle)
SELECT 'TRANSMISSION', 'Transmission au service compétent', 9, 5
WHERE NOT EXISTS (SELECT 1 FROM etape WHERE code_etape = 'TRANSMISSION');
INSERT INTO etape (code_etape, libelle, ordre, duree_previsionnelle)
SELECT 'ARCHIVAGE', 'Archivage du dossier', 10, 3
WHERE NOT EXISTS (SELECT 1 FROM etape WHERE code_etape = 'ARCHIVAGE');

-- ── STATUTS COULEUR ──
INSERT INTO statut_couleur (code, libelle, couleur_hex)
SELECT 'Vert', 'Terminé', '#28a745'
WHERE NOT EXISTS (SELECT 1 FROM statut_couleur WHERE code = 'Vert');
INSERT INTO statut_couleur (code, libelle, couleur_hex)
SELECT 'Jaune', 'En cours', '#ffc107'
WHERE NOT EXISTS (SELECT 1 FROM statut_couleur WHERE code = 'Jaune');
INSERT INTO statut_couleur (code, libelle, couleur_hex)
SELECT 'Rouge', 'Bloqué', '#dc3545'
WHERE NOT EXISTS (SELECT 1 FROM statut_couleur WHERE code = 'Rouge');
INSERT INTO statut_couleur (code, libelle, couleur_hex)
SELECT 'Violet', 'En attente', '#6f42c1'
WHERE NOT EXISTS (SELECT 1 FROM statut_couleur WHERE code = 'Violet');
INSERT INTO statut_couleur (code, libelle, couleur_hex)
SELECT 'Cyan', 'Planifié', '#17a2b8'
WHERE NOT EXISTS (SELECT 1 FROM statut_couleur WHERE code = 'Cyan');
INSERT INTO statut_couleur (code, libelle, couleur_hex)
SELECT 'Orange', 'En retard', '#fd7e14'
WHERE NOT EXISTS (SELECT 1 FROM statut_couleur WHERE code = 'Orange');

-- ── VILLES MALGACHES ──
INSERT INTO ville (nom_ville)
SELECT 'Antananarivo' WHERE NOT EXISTS (SELECT 1 FROM ville WHERE nom_ville = 'Antananarivo');
INSERT INTO ville (nom_ville)
SELECT 'Toamasina' WHERE NOT EXISTS (SELECT 1 FROM ville WHERE nom_ville = 'Toamasina');
INSERT INTO ville (nom_ville)
SELECT 'Antsirabe' WHERE NOT EXISTS (SELECT 1 FROM ville WHERE nom_ville = 'Antsirabe');
INSERT INTO ville (nom_ville)
SELECT 'Fianarantsoa' WHERE NOT EXISTS (SELECT 1 FROM ville WHERE nom_ville = 'Fianarantsoa');
INSERT INTO ville (nom_ville)
SELECT 'Mahajanga' WHERE NOT EXISTS (SELECT 1 FROM ville WHERE nom_ville = 'Mahajanga');
INSERT INTO ville (nom_ville)
SELECT 'Toliara' WHERE NOT EXISTS (SELECT 1 FROM ville WHERE nom_ville = 'Toliara');
INSERT INTO ville (nom_ville)
SELECT 'Antsiranana' WHERE NOT EXISTS (SELECT 1 FROM ville WHERE nom_ville = 'Antsiranana');
INSERT INTO ville (nom_ville)
SELECT 'Nosy Be' WHERE NOT EXISTS (SELECT 1 FROM ville WHERE nom_ville = 'Nosy Be');

-- ── PRIX AU M² (Ariary) ──
INSERT INTO prix_m2 (prix_ttc, prix_ht, observation, date_maj, id_ville)
SELECT 250000.00, 216666.67, 'Prix centre-ville', '2026-01-01', v.id_ville
FROM ville v WHERE v.nom_ville = 'Antananarivo'
  AND NOT EXISTS (SELECT 1 FROM prix_m2 WHERE id_ville = v.id_ville);
INSERT INTO prix_m2 (prix_ttc, prix_ht, observation, date_maj, id_ville)
SELECT 180000.00, 156000.00, 'Prix Toamasina', '2026-01-01', v.id_ville
FROM ville v WHERE v.nom_ville = 'Toamasina'
  AND NOT EXISTS (SELECT 1 FROM prix_m2 WHERE id_ville = v.id_ville);
INSERT INTO prix_m2 (prix_ttc, prix_ht, observation, date_maj, id_ville)
SELECT 150000.00, 130000.00, 'Prix Antsirabe', '2026-01-01', v.id_ville
FROM ville v WHERE v.nom_ville = 'Antsirabe'
  AND NOT EXISTS (SELECT 1 FROM prix_m2 WHERE id_ville = v.id_ville);
INSERT INTO prix_m2 (prix_ttc, prix_ht, observation, date_maj, id_ville)
SELECT 120000.00, 104000.00, 'Prix Fianarantsoa', '2026-01-01', v.id_ville
FROM ville v WHERE v.nom_ville = 'Fianarantsoa'
  AND NOT EXISTS (SELECT 1 FROM prix_m2 WHERE id_ville = v.id_ville);
INSERT INTO prix_m2 (prix_ttc, prix_ht, observation, date_maj, id_ville)
SELECT 200000.00, 173333.33, 'Prix Mahajanga', '2026-01-01', v.id_ville
FROM ville v WHERE v.nom_ville = 'Mahajanga'
  AND NOT EXISTS (SELECT 1 FROM prix_m2 WHERE id_ville = v.id_ville);
INSERT INTO prix_m2 (prix_ttc, prix_ht, observation, date_maj, id_ville)
SELECT 100000.00, 86666.67, 'Prix Toliara', '2026-01-01', v.id_ville
FROM ville v WHERE v.nom_ville = 'Toliara'
  AND NOT EXISTS (SELECT 1 FROM prix_m2 WHERE id_ville = v.id_ville);
INSERT INTO prix_m2 (prix_ttc, prix_ht, observation, date_maj, id_ville)
SELECT 160000.00, 138666.67, 'Prix Antsiranana', '2026-01-01', v.id_ville
FROM ville v WHERE v.nom_ville = 'Antsiranana'
  AND NOT EXISTS (SELECT 1 FROM prix_m2 WHERE id_ville = v.id_ville);
INSERT INTO prix_m2 (prix_ttc, prix_ht, observation, date_maj, id_ville)
SELECT 300000.00, 259000.00, 'Prix Nosy Be (zone touristique)', '2026-01-01', v.id_ville
FROM ville v WHERE v.nom_ville = 'Nosy Be'
  AND NOT EXISTS (SELECT 1 FROM prix_m2 WHERE id_ville = v.id_ville);

-- ============================================================================
-- FIN DE LA MIGRATION V3
-- ============================================================================
