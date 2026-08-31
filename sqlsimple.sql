CREATE TABLE role(
   id_role INT GENERATED ALWAYS AS IDENTITY,
   nom_role VARCHAR(50) NOT NULL,
   description TEXT,
   PRIMARY KEY(id_role),
   UNIQUE(nom_role)
);

CREATE TABLE utilisateur(
   id_utilisateur INT GENERATED ALWAYS AS IDENTITY,
   nom_utilisateur VARCHAR(100) NOT NULL,
   nom_complet VARCHAR(200),
   email VARCHAR(150),
   mot_de_passe_hash VARCHAR(255) NOT NULL,
   actif LOGICAL DEFAULT TRUE,
   statut_compte VARCHAR(30) DEFAULT 'en_attente_activation',
   token_activation VARCHAR(255),
   date_expiration_activation DATETIME,
   tentatives_echouees INT DEFAULT 0,
   verrouille_jusqua DATETIME,
   date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
   derniere_connexion DATETIME,
   PRIMARY KEY(id_utilisateur),
   UNIQUE(nom_utilisateur),
   UNIQUE(email)
);

CREATE TABLE journal_connexion(
   id_journal INT GENERATED ALWAYS AS IDENTITY,
   date_connexion DATETIME DEFAULT CURRENT_TIMESTAMP,
   ip_adresse VARCHAR(50),
   succes LOGICAL DEFAULT TRUE,
   id_utilisateur INT NOT NULL,
   PRIMARY KEY(id_journal),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur)
);

CREATE TABLE module_(
   id_module INT GENERATED ALWAYS AS IDENTITY,
   code_module VARCHAR(50) NOT NULL,
   libelle VARCHAR(100) NOT NULL,
   PRIMARY KEY(id_module),
   UNIQUE(code_module)
);

CREATE TABLE entite(
   id_entite INT GENERATED ALWAYS AS IDENTITY,
   code_entite VARCHAR(50) NOT NULL,
   libelle VARCHAR(100) NOT NULL,
   id_module INT NOT NULL,
   PRIMARY KEY(id_entite),
   UNIQUE(code_entite),
   FOREIGN KEY(id_module) REFERENCES module_(id_module)
);

CREATE TABLE action(
   id_action INT GENERATED ALWAYS AS IDENTITY,
   code_action VARCHAR(50) NOT NULL,
   PRIMARY KEY(id_action),
   UNIQUE(code_action)
);

CREATE TABLE ville(
   id_ville INT GENERATED ALWAYS AS IDENTITY,
   nom_ville VARCHAR(100) NOT NULL,
   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
   updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_ville),
   UNIQUE(nom_ville)
);

CREATE TABLE prix_m2(
   id_prix INT GENERATED ALWAYS AS IDENTITY,
   prix_ttc DECIMAL(14,2) NOT NULL,
   prix_ht DECIMAL(14,2),
   observation VARCHAR(255),
   date_maj DATE DEFAULT CURRENT_DATE,
   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_ville INT NOT NULL,
   PRIMARY KEY(id_prix),
   FOREIGN KEY(id_ville) REFERENCES ville(id_ville)
);

CREATE TABLE statut_dossier(
   id_statut_dossier INT GENERATED ALWAYS AS IDENTITY,
   code VARCHAR(30) NOT NULL,
   libelle VARCHAR(50) NOT NULL,
   couleur_hex VARCHAR(10),
   est_final LOGICAL DEFAULT FALSE,
   ordre INT,
   PRIMARY KEY(id_statut_dossier),
   UNIQUE(code)
);

CREATE TABLE dossier(
   id_dossier INT GENERATED ALWAYS AS IDENTITY,
   numero_dossier VARCHAR(30) NOT NULL,
   date_demande DATE,
   annee INT,
   transmission VARCHAR(50),
   resultat_etude VARCHAR(255),
   observation TEXT,
   date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
   date_modification DATETIME DEFAULT CURRENT_TIMESTAMP,
   date_validation DATE,
   montant_total DECIMAL(16,2) DEFAULT 0,
   id_ville INT NOT NULL,
   id_statut_dossier INT,
   PRIMARY KEY(id_dossier),
   UNIQUE(numero_dossier),
   FOREIGN KEY(id_ville) REFERENCES ville(id_ville),
   FOREIGN KEY(id_statut_dossier) REFERENCES statut_dossier(id_statut_dossier)
);

CREATE TABLE photo(
   id_photo INT GENERATED ALWAYS AS IDENTITY,
   nom_fichier VARCHAR(255),
   chemin_fichier TEXT NOT NULL,
   type_photo VARCHAR(50),
   date_prise DATE,
   date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_photo)
);

CREATE TABLE geometrie(
   id_geometrie INT GENERATED ALWAYS AS IDENTITY,
   type_geometrie VARCHAR(20) NOT NULL,
   geom TEXT NOT NULL,
   precision_m DECIMAL(8,2),
   superficie_calculee_m2 DECIMAL(12,3),
   source VARCHAR(50),
   date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
   date_modification DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_utilisateur INT NOT NULL,
   PRIMARY KEY(id_geometrie),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur)
);

CREATE TABLE etape(
   id_etape INT GENERATED ALWAYS AS IDENTITY,
   code_etape VARCHAR(50) NOT NULL,
   libelle VARCHAR(150) NOT NULL,
   ordre INT NOT NULL,
   duree_previsionnelle INT,
   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_etape),
   UNIQUE(code_etape),
   UNIQUE(ordre)
);

CREATE TABLE statut_couleur(
   id_statut_couleur INT GENERATED ALWAYS AS IDENTITY,
   code VARCHAR(10) NOT NULL,
   libelle VARCHAR(30) NOT NULL,
   couleur_hex VARCHAR(10) NOT NULL,
   PRIMARY KEY(id_statut_couleur),
   UNIQUE(code)
);

CREATE TABLE demandeur(
   id_demandeur INT GENERATED ALWAYS AS IDENTITY,
   nom_prenom VARCHAR(200) NOT NULL,
   contact VARCHAR(100),
   email VARCHAR(100),
   telephone VARCHAR(20),
   adresse TEXT,
   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
   updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_demandeur)
);

CREATE TABLE document_demandeur(
   id_document INT GENERATED ALWAYS AS IDENTITY,
   nom_fichier VARCHAR(255) NOT NULL,
   chemin_fichier TEXT,
   type_document VARCHAR(100) DEFAULT 'Autre',
   taille_octets BIGINT,
   date_upload DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_demandeur INT NOT NULL,
   id_utilisateur INT,
   PRIMARY KEY(id_document),
   FOREIGN KEY(id_demandeur) REFERENCES demandeur(id_demandeur),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur)
);

CREATE TABLE personne(
   id_personne INT GENERATED ALWAYS AS IDENTITY,
   nom VARCHAR(200) NOT NULL,
   contact VARCHAR(100),
   adresse TEXT,
   role VARCHAR(50),
   PRIMARY KEY(id_personne)
);

CREATE TABLE propriete(
   id_propriete INT GENERATED ALWAYS AS IDENTITY,
   nom VARCHAR(200) NOT NULL,
   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
   updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_ville INT,
   PRIMARY KEY(id_propriete),
   UNIQUE(nom),
   FOREIGN KEY(id_ville) REFERENCES ville(id_ville)
);

CREATE TABLE type_signalement(
   id_type_signalement INT GENERATED ALWAYS AS IDENTITY,
   code VARCHAR(50) NOT NULL,
   libelle VARCHAR(100) NOT NULL,
   couleur VARCHAR(10),
   PRIMARY KEY(id_type_signalement),
   UNIQUE(code)
);

CREATE TABLE statut_signalement(
   id_statut_signalement INT GENERATED ALWAYS AS IDENTITY,
   code VARCHAR(30) NOT NULL,
   libelle VARCHAR(50) NOT NULL,
   couleur_hex VARCHAR(10),
   est_final LOGICAL DEFAULT FALSE,
   ordre INT,
   PRIMARY KEY(id_statut_signalement),
   UNIQUE(code)
);

CREATE TABLE session_utilisateur(
   id_session INT GENERATED ALWAYS AS IDENTITY,
   token_rafraichissement VARCHAR(255) NOT NULL,
   user_agent VARCHAR(255),
   ip_adresse VARCHAR(50),
   date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
   date_expiration DATETIME NOT NULL,
   revoque LOGICAL DEFAULT FALSE,
   id_utilisateur INT NOT NULL,
   PRIMARY KEY(id_session),
   UNIQUE(token_rafraichissement),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur)
);

CREATE TABLE alerte(
   id_alerte INT GENERATED ALWAYS AS IDENTITY,
   message TEXT NOT NULL,
   date_envoi DATETIME DEFAULT CURRENT_TIMESTAMP,
   type VARCHAR(50),
   destinataire VARCHAR(200),
   lu LOGICAL DEFAULT FALSE,
   date_lecture DATETIME,
   id_dossier INT,
   PRIMARY KEY(id_alerte),
   FOREIGN KEY(id_dossier) REFERENCES dossier(id_dossier)
);

CREATE TABLE audit(
   id_audit INT GENERATED ALWAYS AS IDENTITY,
   nom_entite VARCHAR(50) NOT NULL,
   id_entite VARCHAR(100) NOT NULL,
   action VARCHAR(30),
   anciennes_valeurs TEXT,
   nouvelles_valeurs TEXT,
   date_action DATETIME DEFAULT CURRENT_TIMESTAMP,
   ip_adresse VARCHAR(50),
   id_utilisateur INT,
   PRIMARY KEY(id_audit),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur)
);

CREATE TABLE document_dossier(
   id_document INT GENERATED ALWAYS AS IDENTITY,
   nom_fichier VARCHAR(255) NOT NULL,
   chemin_fichier TEXT NOT NULL,
   type_document VARCHAR(100),
   taille_octets BIGINT,
   date_upload DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_dossier INT NOT NULL,
   id_utilisateur INT,
   PRIMARY KEY(id_document),
   FOREIGN KEY(id_dossier) REFERENCES dossier(id_dossier),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur)
);

CREATE TABLE permission(
   id_permission INT GENERATED ALWAYS AS IDENTITY,
   id_entite INT NOT NULL,
   id_action INT NOT NULL,
   PRIMARY KEY(id_permission),
   UNIQUE(id_entite, id_action),
   FOREIGN KEY(id_entite) REFERENCES entite(id_entite),
   FOREIGN KEY(id_action) REFERENCES action(id_action)
);

CREATE TABLE suivi_dossier(
   id_suivi INT GENERATED ALWAYS AS IDENTITY,
   date_realisation DATE,
   commentaire TEXT,
   date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
   date_modification DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_etape INT NOT NULL,
   id_dossier INT NOT NULL,
   id_utilisateur INT,
   id_statut_couleur INT,
   PRIMARY KEY(id_suivi),
   UNIQUE(id_dossier, id_etape),
   FOREIGN KEY(id_etape) REFERENCES etape(id_etape),
   FOREIGN KEY(id_dossier) REFERENCES dossier(id_dossier),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur),
   FOREIGN KEY(id_statut_couleur) REFERENCES statut_couleur(id_statut_couleur)
);

CREATE TABLE titre_foncier(
   id_titre_foncier INT GENERATED ALWAYS AS IDENTITY,
   numero VARCHAR(50) NOT NULL,
   type_titre VARCHAR(30) DEFAULT 'Titre Foncier',
   zone VARCHAR(150),
   localisation TEXT,
   superficie_totale DECIMAL(12,3),
   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
   updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_propriete INT NOT NULL,
   PRIMARY KEY(id_titre_foncier),
   UNIQUE(numero, id_propriete),
   FOREIGN KEY(id_propriete) REFERENCES propriete(id_propriete)
);

CREATE TABLE parcelle(
   id_parcelle INT GENERATED ALWAYS AS IDENTITY,
   numero_lot VARCHAR(50),
   superficie_m2 DECIMAL(12,3),
   observation TEXT,
   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
   updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_titre_foncier INT NOT NULL,
   id_utilisateur_creation INT,
   PRIMARY KEY(id_parcelle),
   UNIQUE(numero_lot, id_titre_foncier),
   FOREIGN KEY(id_titre_foncier) REFERENCES titre_foncier(id_titre_foncier),
   FOREIGN KEY(id_utilisateur_creation) REFERENCES utilisateur(id_utilisateur)
);

CREATE TABLE notification_occupation(
   id_notification INT GENERATED ALWAYS AS IDENTITY,
   date_notification DATE,
   annee INT,
   date_convocation DATETIME,
   informations_occupants TEXT,
   constats TEXT,
   doleances TEXT,
   actions_entreprises TEXT,
   statut VARCHAR(30) DEFAULT 'En cours',
   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_parcelle INT,
   id_titre_foncier INT NOT NULL,
   PRIMARY KEY(id_notification),
   FOREIGN KEY(id_parcelle) REFERENCES parcelle(id_parcelle),
   FOREIGN KEY(id_titre_foncier) REFERENCES titre_foncier(id_titre_foncier)
);

CREATE TABLE suivi_notification(
   id_suivi INT GENERATED ALWAYS AS IDENTITY,
   ordre INT NOT NULL,
   date_suivi DATE,
   constats TEXT,
   actions_a_suivre TEXT,
   id_notification INT NOT NULL,
   PRIMARY KEY(id_suivi),
   UNIQUE(id_notification, ordre),
   FOREIGN KEY(id_notification) REFERENCES notification_occupation(id_notification)
);

CREATE TABLE avertissement(
   id_avertissement INT GENERATED ALWAYS AS IDENTITY,
   date_avertissement DATE,
   annee INT,
   informations_occupants TEXT,
   constats TEXT,
   actions_entreprises TEXT,
   a_faire TEXT,
   mission TEXT,
   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_titre_foncier INT NOT NULL,
   id_parcelle INT,
   id_personne INT,
   PRIMARY KEY(id_avertissement),
   FOREIGN KEY(id_titre_foncier) REFERENCES titre_foncier(id_titre_foncier),
   FOREIGN KEY(id_parcelle) REFERENCES parcelle(id_parcelle),
   FOREIGN KEY(id_personne) REFERENCES personne(id_personne)
);

CREATE TABLE dossier_parcelle(
   id_dossier_parcelle INT GENERATED ALWAYS AS IDENTITY,
   superficie_m2 DECIMAL(12,3),
   valeur_demande_ttc DECIMAL(16,2),
   valeur_demande_ht DECIMAL(16,2),
   observation TEXT,
   date_ajout DATETIME DEFAULT CURRENT_TIMESTAMP,
   coordonnee_x DECIMAL(12,3),
   coordonnee_y DECIMAL(12,3),
   id_parcelle INT,
   id_titre_foncier INT NOT NULL,
   id_dossier INT NOT NULL,
   PRIMARY KEY(id_dossier_parcelle),
   FOREIGN KEY(id_parcelle) REFERENCES parcelle(id_parcelle),
   FOREIGN KEY(id_titre_foncier) REFERENCES titre_foncier(id_titre_foncier),
   FOREIGN KEY(id_dossier) REFERENCES dossier(id_dossier)
);

CREATE TABLE descente_terrain(
   id_descente INT GENERATED ALWAYS AS IDENTITY,
   reference VARCHAR(30),
   date_descente DATE,
   statut_constat VARCHAR(50) DEFAULT 'En attente',
   observation TEXT,
   mode VARCHAR(20) DEFAULT 'online',
   validation VARCHAR(30) DEFAULT 'En attente',
   date_validation DATETIME,
   synchronise LOGICAL DEFAULT FALSE,
   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
   date_modification DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_dossier_parcelle INT NOT NULL,
   id_demandeur INT NOT NULL,
   id_utilisateur_validation INT,
   id_utilisateur_creation INT NOT NULL,
   PRIMARY KEY(id_descente),
   UNIQUE(reference),
   FOREIGN KEY(id_dossier_parcelle) REFERENCES dossier_parcelle(id_dossier_parcelle),
   FOREIGN KEY(id_demandeur) REFERENCES demandeur(id_demandeur),
   FOREIGN KEY(id_utilisateur_validation) REFERENCES utilisateur(id_utilisateur),
   FOREIGN KEY(id_utilisateur_creation) REFERENCES utilisateur(id_utilisateur)
);

CREATE TABLE signalement(
   id_signalement INT GENERATED ALWAYS AS IDENTITY,
   reference VARCHAR(30),
   description TEXT,
   date_signalement DATETIME DEFAULT CURRENT_TIMESTAMP,
   date_modification DATETIME DEFAULT CURRENT_TIMESTAMP,
   commentaire_traitement TEXT,
   date_traitement DATETIME,
   id_avertissement INT,
   id_notification INT,
   id_utilisateur_traitement INT,
   id_utilisateur_creation INT NOT NULL,
   id_parcelle INT,
   id_titre_foncier INT,
   id_ville INT,
   id_statut_signalement INT,
   id_type_signalement INT NOT NULL,
   PRIMARY KEY(id_signalement),
   UNIQUE(reference),
   FOREIGN KEY(id_avertissement) REFERENCES avertissement(id_avertissement),
   FOREIGN KEY(id_notification) REFERENCES notification_occupation(id_notification),
   FOREIGN KEY(id_utilisateur_traitement) REFERENCES utilisateur(id_utilisateur),
   FOREIGN KEY(id_utilisateur_creation) REFERENCES utilisateur(id_utilisateur),
   FOREIGN KEY(id_parcelle) REFERENCES parcelle(id_parcelle),
   FOREIGN KEY(id_titre_foncier) REFERENCES titre_foncier(id_titre_foncier),
   FOREIGN KEY(id_ville) REFERENCES ville(id_ville),
   FOREIGN KEY(id_statut_signalement) REFERENCES statut_signalement(id_statut_signalement),
   FOREIGN KEY(id_type_signalement) REFERENCES type_signalement(id_type_signalement)
);

CREATE TABLE utilisateur_role(
   id_utilisateur INT,
   id_role INT,
   date_attribution DATETIME DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_utilisateur, id_role),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur),
   FOREIGN KEY(id_role) REFERENCES role(id_role)
);

CREATE TABLE role_permission(
   id_role INT,
   id_permission INT,
   PRIMARY KEY(id_role, id_permission),
   FOREIGN KEY(id_role) REFERENCES role(id_role),
   FOREIGN KEY(id_permission) REFERENCES permission(id_permission)
);

CREATE TABLE dossier_demandeur(
   id_dossier INT,
   id_demandeur INT,
   role VARCHAR(30) DEFAULT 'Principal',
   date_ajout DATETIME DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_dossier, id_demandeur),
   FOREIGN KEY(id_dossier) REFERENCES dossier(id_dossier),
   FOREIGN KEY(id_demandeur) REFERENCES demandeur(id_demandeur)
);

CREATE TABLE notification_personne(
   id_notification INT,
   id_personne INT,
   role_dans_notification VARCHAR(50) DEFAULT 'Notifié',
   PRIMARY KEY(id_notification, id_personne),
   FOREIGN KEY(id_notification) REFERENCES notification_occupation(id_notification),
   FOREIGN KEY(id_personne) REFERENCES personne(id_personne)
);
