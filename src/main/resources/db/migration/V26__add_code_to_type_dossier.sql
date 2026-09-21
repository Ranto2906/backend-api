-- Ajout de la colonne code à la table type_dossier et données de référence.

ALTER TABLE type_dossier ADD COLUMN code VARCHAR(30) NOT NULL DEFAULT '';
ALTER TABLE type_dossier ADD CONSTRAINT uk_type_dossier_code UNIQUE(code);

-- Mise à jour des données existantes avec les bons codes et libellés
UPDATE type_dossier SET code = 'AUTRE', libelle = 'Autre / non catégorisé' WHERE libelle = 'Autre';

-- Insertion des types manquants
INSERT INTO type_dossier (code, libelle) VALUES
   ('ACHAT', 'Achat de terrain'),
   ('ACTE_VENTE', 'Délivrance / copie d''acte de vente'),
   ('CESSION', 'Cession de terrain'),
   ('RATIFICATION', 'Ratification d''acte de vente'),
   ('OPPOSITION', 'Opposition'),
   ('SIGNATURE_PLAN', 'Signature de plan topographique'),
   ('ATTESTATION', 'Attestation (ni parking ni espace vert, etc.)'),
   ('BORNAGE', 'Bornage'),
   ('APUREMENT', 'Apurement foncier'),
   ('REGULARISATION', 'Régularisation'),
   ('MAINLEVEE', 'Mainlevée d''hypothèque'),
   ('DELIMITATION', 'Délimitation'),
   ('AUTORISATION', 'Autorisation (travaux / transaction / occupation)'),
   ('SERVITUDE', 'Servitude de passage'),
   ('MUTATION', 'Changement de nom / mutation'),
   ('REMBOURSEMENT', 'Remboursement'),
   ('PERMUTATION', 'Permutation de parcelle'),
   ('MISE_DISPO', 'Mise à disposition'),
   ('ECHANGE', 'Échange de terrain')
ON CONFLICT (code) DO NOTHING;
