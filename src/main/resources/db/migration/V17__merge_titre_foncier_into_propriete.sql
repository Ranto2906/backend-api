-- ============================================================================
-- MIGRATION V17 : fusion du titre foncier dans propriete
-- ============================================================================

ALTER TABLE propriete ADD COLUMN IF NOT EXISTS numero VARCHAR(50);
ALTER TABLE propriete ADD COLUMN IF NOT EXISTS zone VARCHAR(150);
ALTER TABLE propriete ADD COLUMN IF NOT EXISTS localisation TEXT;
ALTER TABLE propriete ADD COLUMN IF NOT EXISTS superficie_totale DECIMAL(12,3);

-- Une propriete peut avoir plusieurs anciens titres : le plus ancien fournit
-- les valeurs de référence conservées dans le modèle unifié.
UPDATE propriete p
SET numero = t.numero,
    zone = t.zone,
    localisation = t.localisation,
    superficie_totale = t.superficie_totale
FROM (
    SELECT DISTINCT ON (id_propriete)
           id_propriete, numero, zone, localisation, superficie_totale
    FROM titre_foncier
    ORDER BY id_propriete, created_at NULLS LAST, id_titre_foncier
) t
WHERE p.id_propriete = t.id_propriete;

ALTER TABLE parcelle ADD COLUMN id_propriete_migration INT;
UPDATE parcelle p
SET id_propriete_migration = t.id_propriete
FROM titre_foncier t
WHERE p.id_titre_foncier = t.id_titre_foncier;
ALTER TABLE parcelle DROP CONSTRAINT IF EXISTS parcelle_id_titre_foncier_fkey;
ALTER TABLE parcelle DROP CONSTRAINT IF EXISTS parcelle_numero_lot_id_titre_foncier_key;
ALTER TABLE parcelle DROP COLUMN id_titre_foncier;
ALTER TABLE parcelle RENAME COLUMN id_propriete_migration TO id_propriete;
ALTER TABLE parcelle ADD CONSTRAINT parcelle_id_propriete_fkey
    FOREIGN KEY (id_propriete) REFERENCES propriete(id_propriete);
ALTER TABLE parcelle ALTER COLUMN id_propriete SET NOT NULL;
ALTER TABLE parcelle ADD CONSTRAINT parcelle_numero_lot_id_propriete_key
    UNIQUE (numero_lot, id_propriete);

ALTER TABLE dossier_parcelle ADD COLUMN id_propriete_migration INT;
UPDATE dossier_parcelle d
SET id_propriete_migration = t.id_propriete
FROM titre_foncier t
WHERE d.id_titre_foncier = t.id_titre_foncier;
ALTER TABLE dossier_parcelle DROP CONSTRAINT IF EXISTS dossier_parcelle_id_titre_foncier_fkey;
ALTER TABLE dossier_parcelle DROP CONSTRAINT IF EXISTS dossier_parcelle_id_dossier_id_titre_foncier_id_parcelle_key;
ALTER TABLE dossier_parcelle DROP COLUMN id_titre_foncier;
ALTER TABLE dossier_parcelle RENAME COLUMN id_propriete_migration TO id_propriete;
ALTER TABLE dossier_parcelle ADD CONSTRAINT dossier_parcelle_id_propriete_fkey
    FOREIGN KEY (id_propriete) REFERENCES propriete(id_propriete);
ALTER TABLE dossier_parcelle ADD CONSTRAINT dossier_parcelle_dossier_propriete_parcelle_key
    UNIQUE (id_dossier, id_propriete, id_parcelle);

ALTER TABLE notification_occupation ADD COLUMN id_propriete_migration INT;
UPDATE notification_occupation n
SET id_propriete_migration = t.id_propriete
FROM titre_foncier t
WHERE n.id_titre_foncier = t.id_titre_foncier;
ALTER TABLE notification_occupation DROP CONSTRAINT IF EXISTS notification_occupation_id_titre_foncier_fkey;
ALTER TABLE notification_occupation DROP COLUMN id_titre_foncier;
ALTER TABLE notification_occupation RENAME COLUMN id_propriete_migration TO id_propriete;
ALTER TABLE notification_occupation ADD CONSTRAINT notification_occupation_id_propriete_fkey
    FOREIGN KEY (id_propriete) REFERENCES propriete(id_propriete);
ALTER TABLE notification_occupation ALTER COLUMN id_propriete SET NOT NULL;

ALTER TABLE avertissement ADD COLUMN id_propriete_migration INT;
UPDATE avertissement a
SET id_propriete_migration = t.id_propriete
FROM titre_foncier t
WHERE a.id_titre_foncier = t.id_titre_foncier;
ALTER TABLE avertissement DROP CONSTRAINT IF EXISTS avertissement_id_titre_foncier_fkey;
ALTER TABLE avertissement DROP COLUMN id_titre_foncier;
ALTER TABLE avertissement RENAME COLUMN id_propriete_migration TO id_propriete;
ALTER TABLE avertissement ADD CONSTRAINT avertissement_id_propriete_fkey
    FOREIGN KEY (id_propriete) REFERENCES propriete(id_propriete);
ALTER TABLE avertissement ALTER COLUMN id_propriete SET NOT NULL;

ALTER TABLE signalement ADD COLUMN id_propriete_migration INT;
UPDATE signalement s
SET id_propriete_migration = t.id_propriete
FROM titre_foncier t
WHERE s.id_titre_foncier = t.id_titre_foncier;
ALTER TABLE signalement DROP CONSTRAINT IF EXISTS signalement_id_titre_foncier_fkey;
ALTER TABLE signalement DROP COLUMN id_titre_foncier;
ALTER TABLE signalement RENAME COLUMN id_propriete_migration TO id_propriete;
ALTER TABLE signalement ADD CONSTRAINT signalement_id_propriete_fkey
    FOREIGN KEY (id_propriete) REFERENCES propriete(id_propriete);

-- Les audits conservent leur historique, mais avec le nouveau type et la clé
-- de propriete. Les géométries polymorphes d'anciens titres ne peuvent pas
-- être converties dans leur colonne UUID et sont supprimées avant le DROP.
UPDATE audit a
SET entite_type = 'propriete', entite_id = t.id_propriete::text
FROM titre_foncier t
WHERE a.entite_type = 'titre_foncier'
  AND a.entite_id = t.id_titre_foncier::text;
DELETE FROM geometrie WHERE entite_type = 'titre_foncier';

DROP TABLE titre_foncier;

-- L'entité d'autorisation historique n'est plus exposée par l'API.
DELETE FROM role_permission
WHERE id_permission IN (
    SELECT p.id_permission
    FROM permission p
    JOIN entite e ON e.id_entite = p.id_entite
    WHERE e.code_entite = 'TITRE_FONCIER'
);
DELETE FROM permission
WHERE id_entite IN (SELECT id_entite FROM entite WHERE code_entite = 'TITRE_FONCIER');
DELETE FROM entite WHERE code_entite = 'TITRE_FONCIER';

ALTER TABLE audit DROP CONSTRAINT IF EXISTS audit_entite_type_check;
ALTER TABLE audit ADD CONSTRAINT audit_entite_type_check
CHECK(entite_type IN('dossier','notification_occupation','avertissement',
                     'parcelle','propriete','utilisateur','geometrie',
                     'signalement','descente_terrain','personne','ville',
                     'prix_m2','role','suivi_notification','assignation'));

ALTER TABLE geometrie DROP CONSTRAINT IF EXISTS geometrie_entite_type_check;
ALTER TABLE geometrie ADD CONSTRAINT geometrie_entite_type_check
CHECK(entite_type IN('parcelle','dossier_parcelle','signalement','descente_terrain'));