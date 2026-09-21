-- Remplace la relation directe propriété -> ville par propriété -> lieu -> ville.

CREATE TABLE lieu(
   id_lieu INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   libelle VARCHAR(150) NOT NULL,
   id_ville INT NOT NULL REFERENCES ville(id_ville),
   UNIQUE(libelle, id_ville)
);

INSERT INTO lieu (libelle, id_ville)
SELECT v.nom_ville, v.id_ville
FROM ville v
WHERE EXISTS (SELECT 1 FROM propriete p WHERE p.id_ville = v.id_ville);

ALTER TABLE propriete ADD COLUMN id_lieu INT REFERENCES lieu(id_lieu);

UPDATE propriete p
SET id_lieu = l.id_lieu
FROM lieu l
WHERE l.id_ville = p.id_ville
  AND l.libelle = (SELECT v.nom_ville FROM ville v WHERE v.id_ville = p.id_ville);

CREATE INDEX idx_lieu_ville ON lieu(id_ville);
CREATE INDEX idx_propriete_lieu ON propriete(id_lieu);

ALTER TABLE propriete DROP COLUMN id_ville;