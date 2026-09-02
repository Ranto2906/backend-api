-- ============================================================================
-- BASE DE DONNEES : Patrimoine foncier (Demandes + Suivi + Signalements +
--                    Descentes terrain + Carte)
-- PostgreSQL 14+ avec extension PostGIS
-- ============================================================================
--
-- CORRECTIONS APPLIQUEES DANS CETTE VERSION :
--  1. dossier.etat (VARCHAR libre)          -> table statut_dossier (id_statut_dossier)
--  2. suivi_dossier.statut_couleur (VARCHAR) -> table statut_couleur (id_statut_couleur)
--  3. signalement : id_titre_foncier/id_parcelle/id_ville/id_dossier redevenus
--     nullable (un signalement existe avant d'être rattaché à un terrain) ;
--     id_signalement en UUID ; id_utilisateur scindé en création/traitement ;
--     description en TEXT ; commentaire_traitement/date_traitement restaurés ;
--     id_notification/id_avertissement restaurés (rattachement si "Transformé")
--  4. descente_terrain : id_descente/synchronise correctement typés (UUID/BOOLEAN) ;
--     id_utilisateur_1 -> id_utilisateur_validation ; id_dossier+id_parcelle+
--     id_titre_foncier remplacés par id_dossier_parcelle (pointe la ligne exacte)
--  5. photo / geometrie / audit : CHECK élargis à 'signalement' et 'descente_terrain'
--
-- Règle d'identifiants (inchangée) :
--  - INTEGER GENERATED ALWAYS AS IDENTITY : tables administratives, jamais
--    créées hors-ligne ni référencées en polymorphe (photo/geometrie/audit)
--  - UUID DEFAULT gen_random_uuid()       : tables créables hors-ligne (mobile)
--    OU pouvant être ciblées par entite_type/entite_id (photo, geometrie)
--    Rappel : côté Spring Boot, ces UUID sont générés par le CLIENT (mobile),
--    pas par la base -> le DEFAULT n'est qu'un filet de sécurité, jamais utilisé
--    en pratique par l'API (voir entités JPA sans @GeneratedValue).
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS postgis;    -- types et fonctions géospatiales

-- ============================================================================
-- MODULE 0 : UTILISATEURS ET DROITS D'ACCES
-- ============================================================================

CREATE TABLE role(
   id_role INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   nom_role VARCHAR(50) NOT NULL,
   description TEXT,
   UNIQUE(nom_role)
);

CREATE TABLE utilisateur(
   id_utilisateur INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   nom_utilisateur VARCHAR(100) NOT NULL,
   nom_complet VARCHAR(200),
   email VARCHAR(150),
   mot_de_passe_hash VARCHAR(255) NOT NULL,
   actif BOOLEAN DEFAULT TRUE,
   statut_compte VARCHAR(20) DEFAULT 'en_attente_activation'
      CHECK(statut_compte IN('en_attente_activation','actif','desactive')),
   token_activation VARCHAR(255),
   date_expiration_activation TIMESTAMP,
   tentatives_echouees INT DEFAULT 0,
   verrouille_jusqu_a TIMESTAMP,
   date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   derniere_connexion TIMESTAMP,
   UNIQUE(nom_utilisateur),
   UNIQUE(email)
);

CREATE TABLE journal_connexion(
   id_journal INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   date_connexion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   ip_adresse INET,
   succes BOOLEAN DEFAULT TRUE,
   id_utilisateur INT NOT NULL REFERENCES utilisateur(id_utilisateur)
);

CREATE TABLE session_utilisateur(
   id_session INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   token_rafraichissement VARCHAR(255) NOT NULL UNIQUE,
   user_agent VARCHAR(255),
   ip_adresse INET,
   date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   date_expiration TIMESTAMP NOT NULL,
   revoque BOOLEAN DEFAULT FALSE,
   id_utilisateur INT NOT NULL REFERENCES utilisateur(id_utilisateur)
);

CREATE TABLE module_(
   id_module INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   code_module VARCHAR(50) NOT NULL,
   libelle VARCHAR(100) NOT NULL,
   UNIQUE(code_module)
);

CREATE TABLE entite(
   id_entite INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   code_entite VARCHAR(50) NOT NULL,
   libelle VARCHAR(100) NOT NULL,
   id_module INT NOT NULL REFERENCES module_(id_module),
   UNIQUE(code_entite)
);

CREATE TABLE action(
   id_action INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   code_action VARCHAR(50) NOT NULL,
   UNIQUE(code_action)
);

CREATE TABLE permission(
   id_permission INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   id_action INT NOT NULL REFERENCES action(id_action),
   id_entite INT NOT NULL REFERENCES entite(id_entite),
   UNIQUE(id_entite, id_action)
);

CREATE TABLE role_permission(
   id_role INT REFERENCES role(id_role),
   id_permission INT REFERENCES permission(id_permission),
   PRIMARY KEY(id_role, id_permission)
);

CREATE TABLE utilisateur_role(
   id_utilisateur INT REFERENCES utilisateur(id_utilisateur),
   id_role INT REFERENCES role(id_role),
   date_attribution TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_utilisateur, id_role)
);

-- ============================================================================
-- MODULE 1 : REFERENTIEL VILLE / PRIX
-- ============================================================================

CREATE TABLE ville(
   id_ville INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   nom_ville VARCHAR(100) NOT NULL,
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   UNIQUE(nom_ville)
);

CREATE TABLE prix_m2(
   id_prix INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   prix_ttc DECIMAL(14,2) NOT NULL,
   prix_ht DECIMAL(14,2),
   observation VARCHAR(255),
   date_maj DATE NOT NULL DEFAULT CURRENT_DATE,
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   id_ville INT NOT NULL REFERENCES ville(id_ville)
);

-- ============================================================================
-- MODULE 2 : REFERENTIEL TERRAIN (partagé Demandes + Suivi + Signalements)
-- UUID : parcelle/titre_foncier sont ciblés en polymorphe par geometrie/photo
-- ============================================================================

CREATE TABLE propriete(
   id_propriete INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   nom VARCHAR(200) NOT NULL,
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   id_ville INT REFERENCES ville(id_ville),
   UNIQUE(nom)
);

CREATE TABLE titre_foncier(
   id_titre_foncier UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   numero VARCHAR(50) NOT NULL,
   type_titre VARCHAR(30) DEFAULT 'Titre Foncier'
      CHECK(type_titre IN('Titre Foncier','Parcelle Cadastrale')),
   zone VARCHAR(150),
   localisation TEXT,
   superficie_totale DECIMAL(12,3),
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   id_propriete INT NOT NULL REFERENCES propriete(id_propriete),
   UNIQUE(numero, id_propriete)
);

CREATE TABLE parcelle(
   id_parcelle UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   numero_lot VARCHAR(50),
   superficie_m2 DECIMAL(12,3),
   observation TEXT,
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   id_utilisateur_creation INT REFERENCES utilisateur(id_utilisateur),
   id_titre_foncier UUID NOT NULL REFERENCES titre_foncier(id_titre_foncier),
   UNIQUE(numero_lot, id_titre_foncier)
);

-- ============================================================================
-- MODULE 3 : DOSSIER (demandes d'acquisition de terrain)
-- ============================================================================

-- CORRECTION 1 : remplace dossier.etat VARCHAR(50) libre.
-- est_final : déclenche la promotion du point dossier_parcelle -> geometrie
CREATE TABLE statut_dossier(
   id_statut_dossier INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   code VARCHAR(30) NOT NULL,
   libelle VARCHAR(50) NOT NULL,
   couleur_hex VARCHAR(10) DEFAULT '#8a8578',
   est_final BOOLEAN DEFAULT FALSE,
   ordre INT,
   UNIQUE(code)
);

CREATE TABLE etape(
   id_etape INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   code_etape VARCHAR(50) NOT NULL,
   libelle VARCHAR(150) NOT NULL,
   ordre INT NOT NULL,
   duree_previsionnelle INT,
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   UNIQUE(code_etape),
   UNIQUE(ordre)
);

CREATE TABLE dossier(
   id_dossier INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   numero_dossier VARCHAR(30) NOT NULL,
   date_demande DATE,
   annee INT,
   transmission VARCHAR(50),
   resultat_etude VARCHAR(255),
   observation TEXT,
   date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   date_validation DATE,
   montant_total DECIMAL(16,2) DEFAULT 0,
   id_statut_dossier INT REFERENCES statut_dossier(id_statut_dossier),  -- CORRECTION 1
   id_ville INT NOT NULL REFERENCES ville(id_ville)
);

CREATE TABLE demandeur(
   id_demandeur INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   nom_prenom VARCHAR(200) NOT NULL,
   contact VARCHAR(100),
   email VARCHAR(100),
   telephone VARCHAR(20),
   adresse TEXT,
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dossier_demandeur(
   id_dossier INT REFERENCES dossier(id_dossier),
   id_demandeur INT REFERENCES demandeur(id_demandeur),
   role VARCHAR(30) DEFAULT 'Principal',
   date_ajout TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_dossier, id_demandeur)
);

-- id_dossier_parcelle : clé simple -> nécessaire pour que geometrie et
-- descente_terrain puissent pointer vers une ligne précise
CREATE TABLE dossier_parcelle(
   id_dossier_parcelle UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   id_dossier INT NOT NULL REFERENCES dossier(id_dossier),
   id_titre_foncier UUID NOT NULL REFERENCES titre_foncier(id_titre_foncier),
   id_parcelle UUID REFERENCES parcelle(id_parcelle),
   superficie_m2 DECIMAL(12,3),
   valeur_demande_ttc DECIMAL(16,2),
   valeur_demande_ht DECIMAL(16,2),
   observation TEXT,
   date_ajout TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   coordonnee_x_ DECIMAL(12,3),      -- brut Laborde, saisi au bureau
   coordonnee_y_ DECIMAL(12,3),
   UNIQUE(id_dossier, id_titre_foncier, id_parcelle)
);

-- CORRECTION 2 : remplace suivi_dossier.statut_couleur VARCHAR(10) libre.
-- Réutilisable ailleurs si un autre module a besoin d'une pastille colorée.
CREATE TABLE statut_couleur(
   id_statut_couleur INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   code VARCHAR(10) NOT NULL,
   libelle VARCHAR(30) NOT NULL,
   couleur_hex VARCHAR(10) NOT NULL,
   UNIQUE(code)
);

CREATE TABLE suivi_dossier(
   id_suivi INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   date_realisation DATE,
   id_statut_couleur INT REFERENCES statut_couleur(id_statut_couleur),  -- CORRECTION 2
   commentaire TEXT,
   date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   id_utilisateur INT REFERENCES utilisateur(id_utilisateur),
   id_dossier INT NOT NULL REFERENCES dossier(id_dossier),
   id_etape INT NOT NULL REFERENCES etape(id_etape),
   UNIQUE(id_dossier, id_etape)
);

CREATE TABLE document_dossier(
   id_document INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   nom_fichier VARCHAR(255) NOT NULL,
   chemin_fichier TEXT NOT NULL,
   type_document VARCHAR(100),
   taille_octets BIGINT,
   date_upload TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   id_utilisateur INT REFERENCES utilisateur(id_utilisateur),
   id_dossier INT NOT NULL REFERENCES dossier(id_dossier)
);

CREATE TABLE document_demandeur(
   id_document INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   nom_fichier VARCHAR(255) NOT NULL,
   chemin_fichier TEXT,
   type_document VARCHAR(100) NOT NULL DEFAULT 'Autre'
      CHECK(type_document IN('photo_demandeur','photo_identite','pdf_piece','PV','Plan','Quittance','Acte','Autre')),
   taille_octets BIGINT,
   date_upload TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   id_utilisateur INT REFERENCES utilisateur(id_utilisateur),
   id_demandeur INT NOT NULL REFERENCES demandeur(id_demandeur)
);

CREATE TABLE alerte(
   id_alerte INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   message TEXT NOT NULL,
   date_envoi TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   type VARCHAR(50),
   destinataire VARCHAR(200),
   lu BOOLEAN DEFAULT FALSE,
   date_lecture TIMESTAMP,
   id_dossier INT REFERENCES dossier(id_dossier)
);

-- entite_id en TEXT : les tables auditées mélangent des PK INTEGER (dossier,
-- utilisateur, propriete) et UUID (parcelle, titre_foncier, notification_occupation,
-- avertissement, signalement, descente_terrain, geometrie) -> TEXT accepte les deux.
CREATE TABLE audit(
   id_audit INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   entite_type VARCHAR(30) NOT NULL
      CHECK(entite_type IN('dossier','notification_occupation','avertissement',
                            'parcelle','titre_foncier','propriete','utilisateur',
                            'geometrie','signalement','descente_terrain','personne')),   -- CORRECTION 5 + personne
   entite_id TEXT NOT NULL,
   action VARCHAR(20),
   anciennes_valeurs JSONB,
   nouvelles_valeurs JSONB,
   date_action TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   ip_adresse INET,
   id_utilisateur INT REFERENCES utilisateur(id_utilisateur)
);

-- ============================================================================
-- MODULE 4 : SUIVI DES NOTIFICATIONS / AVERTISSEMENTS
-- UUID : ciblées en polymorphe par photo (entite_type='notification'/'avertissement')
-- ============================================================================

CREATE TABLE personne(
   id_personne INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   nom VARCHAR(200) NOT NULL,
   contact VARCHAR(100),
   adresse TEXT,
   role VARCHAR(50)
);

CREATE TABLE notification_occupation(
   id_notification UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   date_notification DATE,
   annee INT,
   date_convocation TIMESTAMP,
   informations_occupants TEXT,
   constats TEXT,
   doleances TEXT,
   actions_entreprises TEXT,
   statut VARCHAR(30) DEFAULT 'En cours',
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   id_parcelle UUID REFERENCES parcelle(id_parcelle),
   id_titre_foncier UUID NOT NULL REFERENCES titre_foncier(id_titre_foncier)
);

CREATE TABLE notification_personne(
   id_notification UUID REFERENCES notification_occupation(id_notification),
   id_personne INT REFERENCES personne(id_personne),
   role_dans_notification VARCHAR(50) DEFAULT 'Notifié',
   PRIMARY KEY(id_notification, id_personne)
);

CREATE TABLE suivi_notification(
   id_suivi UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   ordre INT NOT NULL,
   date_suivi DATE,
   constats TEXT,
   actions_a_suivre TEXT,
   id_notification UUID NOT NULL REFERENCES notification_occupation(id_notification),
   UNIQUE(id_notification, ordre)
);

CREATE TABLE avertissement(
   id_avertissement UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   date_avertissement DATE,
   annee INT,
   informations_occupants TEXT,
   constats TEXT,
   actions_entreprises TEXT,
   a_faire TEXT,
   mission TEXT,
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   id_personne INT REFERENCES personne(id_personne),
   id_parcelle UUID REFERENCES parcelle(id_parcelle),
   id_titre_foncier UUID NOT NULL REFERENCES titre_foncier(id_titre_foncier)
);

CREATE TABLE photo(
   id_photo UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   entite_type VARCHAR(30) NOT NULL
      CHECK(entite_type IN('notification','suivi','avertissement','signalement','descente_terrain')),  -- CORRECTION 5
   entite_id UUID NOT NULL,
   type_photo VARCHAR(50),
   chemin_fichier TEXT,
   date_prise DATE,
   id_utilisateur INT REFERENCES utilisateur(id_utilisateur),
   date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_photo_entite ON photo(entite_type, entite_id);

-- ============================================================================
-- MODULE 5 : CARTE (geometrie via PostGIS)
-- ============================================================================

CREATE TABLE geometrie(
   id_geometrie UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   entite_type VARCHAR(30) NOT NULL
      CHECK(entite_type IN('parcelle','titre_foncier','dossier_parcelle','signalement','descente_terrain')),  -- CORRECTION 5
   entite_id UUID NOT NULL,
   type_geometrie VARCHAR(20) NOT NULL CHECK(type_geometrie IN('Point','Polygon','LineString')),
   geom geography(Geometry, 4326) NOT NULL,   -- toujours en WGS84 (règle RG3)
   precision_m DECIMAL(8,2),                  -- précision GPS si source mobile
   superficie_calculee_m2 DECIMAL(12,3),
   source VARCHAR(30) DEFAULT 'Tracé manuel'
      CHECK(source IN('Tracé manuel','Import GPS','Calculé automatiquement','Point carte','Recherche de lieu')),
   date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   id_utilisateur INT NOT NULL REFERENCES utilisateur(id_utilisateur),
   UNIQUE(entite_type, entite_id)
);
CREATE INDEX idx_geometrie_entite ON geometrie(entite_type, entite_id);
CREATE INDEX idx_geometrie_geom ON geometrie USING GIST(geom);

-- ============================================================================
-- MODULE 6 : SIGNALEMENTS (soumis mobile, gérés web)
-- UUID : créés hors-ligne sur le mobile
-- ============================================================================

CREATE TABLE type_signalement(
   id_type_signalement INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   code VARCHAR(50) NOT NULL,
   libelle VARCHAR(100) NOT NULL,
   couleur VARCHAR(10) DEFAULT '#a13a2c',
   UNIQUE(code)
);

-- table de statut (déjà votre bonne intuition) -> alignée / complétée
CREATE TABLE statut_signalement(
   id_statut_signalement INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   code VARCHAR(30) NOT NULL,
   libelle VARCHAR(50) NOT NULL,
   couleur_hex VARCHAR(10) DEFAULT '#8a8578',
   est_final BOOLEAN DEFAULT FALSE,   -- ex: 'Rejeté', 'Transformé'
   ordre INT,
   UNIQUE(code)
);

-- CORRECTION 3 : id_titre_foncier/id_parcelle/id_ville/id_dossier redevenus
-- nullable (rattachement possible après coup, côté web) ; UUID ; utilisateur
-- création/traitement distincts ; commentaire et date de traitement restaurés.
CREATE TABLE signalement(
   id_signalement UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   reference VARCHAR(30) UNIQUE,
   description TEXT,                                   -- CORRECTION 3 : TEXT, pas VARCHAR(50)
   date_signalement TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   id_type_signalement INT NOT NULL REFERENCES type_signalement(id_type_signalement),
   id_statut_signalement INT REFERENCES statut_signalement(id_statut_signalement),
   id_ville INT REFERENCES ville(id_ville),                        -- nullable
   id_titre_foncier UUID REFERENCES titre_foncier(id_titre_foncier), -- nullable
   id_parcelle UUID REFERENCES parcelle(id_parcelle),               -- nullable
   commentaire_traitement TEXT,
   date_traitement TIMESTAMP,
   id_utilisateur_traitement INT REFERENCES utilisateur(id_utilisateur),
   id_dossier INT REFERENCES dossier(id_dossier),                   -- nullable, rempli si "Transformé"
   id_notification UUID REFERENCES notification_occupation(id_notification),
   id_avertissement UUID REFERENCES avertissement(id_avertissement),
   id_utilisateur_creation INT NOT NULL REFERENCES utilisateur(id_utilisateur)
);
CREATE INDEX idx_signalement_statut ON signalement(id_statut_signalement);
CREATE INDEX idx_signalement_ville ON signalement(id_ville);

-- ============================================================================
-- MODULE 7 : DESCENTE TERRAIN (vérification avant-vente, indépendante des étapes)
-- UUID : créée hors-ligne sur le mobile, comme signalement
-- ============================================================================

-- CORRECTION 4 : id_descente en UUID, synchronise en BOOLEAN,
-- id_utilisateur_1 -> id_utilisateur_validation, et id_dossier+id_parcelle+
-- id_titre_foncier remplacés par id_dossier_parcelle (pointe la ligne exacte
-- vérifiée, utile quand un dossier porte sur plusieurs parcelles).
CREATE TABLE descente_terrain(
   id_descente UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   reference VARCHAR(30) UNIQUE,
   date_descente DATE,
   statut_constat VARCHAR(50) DEFAULT 'En attente'
      CHECK(statut_constat IN('Conforme','Non conforme','En attente','Occupation illicite','Construction illegale')),
   observation TEXT,
   mode VARCHAR(20) DEFAULT 'online' CHECK(mode IN('online','offline')),
   validation VARCHAR(30) DEFAULT 'En attente'
      CHECK(validation IN('En attente','Valide','Rejete','Complement demande')),
   date_validation TIMESTAMP,
   synchronise BOOLEAN DEFAULT FALSE,
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   id_utilisateur_creation INT NOT NULL REFERENCES utilisateur(id_utilisateur),
   id_utilisateur_validation INT REFERENCES utilisateur(id_utilisateur),
   id_demandeur INT NOT NULL REFERENCES demandeur(id_demandeur),
   id_dossier_parcelle UUID NOT NULL REFERENCES dossier_parcelle(id_dossier_parcelle)
);
CREATE INDEX idx_descente_dossier_parcelle ON descente_terrain(id_dossier_parcelle);

-- ============================================================================
-- FIN DU SCRIPT
-- ============================================================================