-- ============================================================================
-- Provinces de Madagascar (table ville) + lieux extraits de
-- Situation_des_demandes_Patrimoines.xlsx, triés par province (table lieu)
-- Idempotent : n'insère que les lignes manquantes (lieux déjà créés par
-- l'import précédent ne sont pas dupliqués).
-- ============================================================================

-- 1) Les 6 provinces traditionnelles (ignorées si déjà présentes)
INSERT INTO ville (nom_ville)
SELECT v.nom_ville FROM (VALUES
   ('Antananarivo'),
   ('Fianarantsoa'),
   ('Toamasina'),
   ('Mahajanga'),
   ('Toliara'),
   ('Antsiranana')
) AS v(nom_ville)
WHERE NOT EXISTS (SELECT 1 FROM ville x WHERE x.nom_ville = v.nom_ville);

-- 2) Les lieux, rattachés à leur province via une sous-requête sur nom_ville
--    (insérés seulement si le couple libelle/province n'existe pas encore)

-- --- Province : Antananarivo (91 lieu(x)) ---
INSERT INTO lieu (libelle, id_ville)
SELECT t.libelle, (SELECT id_ville FROM ville WHERE nom_ville = 'Antananarivo')
FROM (VALUES
   ('67 Ha'),
   ('67 Ha Nord Est'),
   ('67 Nord Est'),
   ('67 ha Nord Est'),
   ('67 ha Sud'),
   ('67Ha'),
   ('67Ha CO'),
   ('67Ha Centre Ouest'),
   ('67Ha II'),
   ('67Ha Sud'),
   ('67ha'),
   ('67ha Centre Ouest'),
   ('67ha Nord Est'),
   ('67ha Nord Ouest'),
   ('67ha Sud'),
   ('67ha centre Ouest'),
   ('Ambatolampy Ambanimaso I'),
   ('Ambatomena Anosiala'),
   ('Ambodin''Isotry'),
   ('Ambodin''isotry'),
   ('Ambohijatovo'),
   ('Ambohipo'),
   ('Ambositra'),
   ('Ampefiloha'),
   ('Anatihazo'),
   ('Anatihazo Isotry'),
   ('Andavamamba Anatihazo I'),
   ('Andavamamba Antetezanafovoany I'),
   ('Andohatampenaka I'),
   ('Andohatapenaka'),
   ('Andohatapenaka I'),
   ('Andohatapenaka III'),
   ('Andranomahery'),
   ('Andranomahery B2'),
   ('AndranomaherySection B'),
   ('Andranomanalina I'),
   ('Andranomanalina Isotry'),
   ('Andravoahangy'),
   ('Andravoahangy Tsena'),
   ('Anjanahary'),
   ('Anjezika'),
   ('Anjezika Antetezanafovoany Isotry'),
   ('Anjezika Avaratra'),
   ('Anjezika Isotry'),
   ('Ankasina'),
   ('Ankasina / Anjezika'),
   ('Ankasina / Ankasina / Ankasina / Antohomadinika / Antohomadinika'),
   ('Ankasina / Antetezanafovoany'),
   ('Ankorondrano Andranomahery'),
   ('Anosiala'),
   ('Anosilava'),
   ('Anosipatrana'),
   ('Anosizato Atsinanana'),
   ('Anosizato Est'),
   ('Antetezana Afovoany'),
   ('Antetezana afovoany Isotry'),
   ('Antetezanafovoany'),
   ('Antetezanafovoany Isotry'),
   ('Antohomadinika'),
   ('Antohomadinika Ankasina'),
   ('Antohomadinika FAAMI'),
   ('Antohomadinika Isotry'),
   ('Antohomadinika Sud'),
   ('Antohomadinika afovoany'),
   ('Antsahalovana'),
   ('Antsalovana'),
   ('Betsizaraina'),
   ('Cité Ambodinisotry'),
   ('Cité SEIMAD Mandrimena Andoharanofotsy'),
   ('Coum 67ha'),
   ('Ivato'),
   ('Manakambahiny'),
   ('Manarintsoa'),
   ('Manazary Betsizaraina'),
   ('Manazary Ilafy'),
   ('Manazary parcelle 01'),
   ('Mandrosoa Ivato'),
   ('Namonatana'),
   ('Namontana'),
   ('Nosibe nord'),
   ('Ouest Manjakaray'),
   ('Ouest d’Anjanahary (Andravoahangy)'),
   ('Parcelle N°2 / Andranomanga Anosiala'),
   ('Parcelle n°2 / Andranomanga Anosiala'),
   ('Section B Andranomahery / Registre 15 Folio 14 / Canton Ambatomainty / Ankorondrano'),
   ('Section B Andranomahery Registre 15 Folio 6 Ankorondrano'),
   ('Soanierana'),
   ('Tanambao 67ha Sud'),
   ('Tsarakofafa'),
   ('Tsimialonandro Andohatany (Andravoahangy)'),
   ('Vontovorona')
) AS t(libelle)
WHERE NOT EXISTS (
   SELECT 1 FROM lieu l
   WHERE l.libelle = t.libelle
     AND l.id_ville = (SELECT id_ville FROM ville WHERE nom_ville = 'Antananarivo')
);

-- --- Province : Fianarantsoa (2 lieu(x)) ---
INSERT INTO lieu (libelle, id_ville)
SELECT t.libelle, (SELECT id_ville FROM ville WHERE nom_ville = 'Fianarantsoa')
FROM (VALUES
   ('Farafangana'),
   ('Mananjary')
) AS t(libelle)
WHERE NOT EXISTS (
   SELECT 1 FROM lieu l
   WHERE l.libelle = t.libelle
     AND l.id_ville = (SELECT id_ville FROM ville WHERE nom_ville = 'Fianarantsoa')
);

-- --- Province : Toamasina (5 lieu(x)) ---
INSERT INTO lieu (libelle, id_ville)
SELECT t.libelle, (SELECT id_ville FROM ville WHERE nom_ville = 'Toamasina')
FROM (VALUES
   ('Mangarano II'),
   ('Tanambao V'),
   ('Toamasina ville'),
   ('Tsarakofafa'),
   ('Vatomandry')
) AS t(libelle)
WHERE NOT EXISTS (
   SELECT 1 FROM lieu l
   WHERE l.libelle = t.libelle
     AND l.id_ville = (SELECT id_ville FROM ville WHERE nom_ville = 'Toamasina')
);

-- --- Province : Mahajanga (8 lieu(x)) ---
INSERT INTO lieu (libelle, id_ville)
SELECT t.libelle, (SELECT id_ville FROM ville WHERE nom_ville = 'Mahajanga')
FROM (VALUES
   ('Ankaraobato'),
   ('Antanambao SOTEMA'),
   ('Antanimalandy'),
   ('Cité 143 TSARAMANDROSO'),
   ('Cité Tsaramandroso mahavoky sud'),
   ('Mahavoky Tsaramandroso'),
   ('Manjarisoa'),
   ('logement 330 Cité  TSARAMANDROSO')
) AS t(libelle)
WHERE NOT EXISTS (
   SELECT 1 FROM lieu l
   WHERE l.libelle = t.libelle
     AND l.id_ville = (SELECT id_ville FROM ville WHERE nom_ville = 'Mahajanga')
);

-- --- Province : Toliara (3 lieu(x)) ---
INSERT INTO lieu (libelle, id_ville)
SELECT t.libelle, (SELECT id_ville FROM ville WHERE nom_ville = 'Toliara')
FROM (VALUES
   ('Ankilifaly Toliara I'),
   ('TOLIARA'),
   ('Toliara')
) AS t(libelle)
WHERE NOT EXISTS (
   SELECT 1 FROM lieu l
   WHERE l.libelle = t.libelle
     AND l.id_ville = (SELECT id_ville FROM ville WHERE nom_ville = 'Toliara')
);

-- --- Province : Antsiranana (2 lieu(x)) ---
INSERT INTO lieu (libelle, id_ville)
SELECT t.libelle, (SELECT id_ville FROM ville WHERE nom_ville = 'Antsiranana')
FROM (VALUES
   ('Antsiranana'),
   ('Marofinaritra')
) AS t(libelle)
WHERE NOT EXISTS (
   SELECT 1 FROM lieu l
   WHERE l.libelle = t.libelle
     AND l.id_ville = (SELECT id_ville FROM ville WHERE nom_ville = 'Antsiranana')
);
