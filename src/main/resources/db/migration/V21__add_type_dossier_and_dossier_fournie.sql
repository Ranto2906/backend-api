-- Ajout des référentiels de type de dossier et des pièces fournies.

CREATE TABLE type_dossier(
   id_type_dossier INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   libelle VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE dossier_fournie(
   id_dossier_fournie INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   libelle VARCHAR(100) NOT NULL UNIQUE
);

ALTER TABLE dossier
   ADD COLUMN id_type_dossier INT REFERENCES type_dossier(id_type_dossier),
   ADD COLUMN id_dossier_fournie INT REFERENCES dossier_fournie(id_dossier_fournie);

CREATE INDEX idx_dossier_type_dossier ON dossier(id_type_dossier);
CREATE INDEX idx_dossier_dossier_fournie ON dossier(id_dossier_fournie);