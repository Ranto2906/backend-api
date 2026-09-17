-- ============================================================================
-- V20 : enregistrement du SRID Laborde (grille malgache) pour la conversion
-- des coordonnées X/Y du fichier Excel « Situation des demandes Patrimoines »
-- vers le WGS84 (EPSG:4326) lors de l'insertion dans la table geometrie.
--
-- REMARQUE IMPORTANTE : la définition officielle EPSG:29701 est « Tananarive
-- 1925 / UTM zone 39S » et NE CORRESPOND PAS à la grille Laborde utilisée
-- dans les fichiers Excel (vérification : x=505539, y=811906 → région
-- d'Antananarivo avec la grille Laborde ; impossible en UTM).
-- On enregistre donc la projection Laborde sous un SRID personnalisé 900914
-- (plage réservée aux définitions non-EPSG). Approximation LCC de la grille
-- Laborde (celle utilisée par la communauté géomatique malgache, précision
-- métrique à décamétrique) :
--   LCC, lat_0 = lat_1 = 18°58' S, lon_0 = 46°30' E,
--   k_0 = 0.9995, x_0 = 400 000 m, y_0 = 800 000 m, ellipsoïde international.
-- ============================================================================

INSERT INTO spatial_ref_sys (srid, auth_name, auth_srid, srtext, proj4text)
VALUES (
  900914,
  'SEIMAD',
  900914,
  'PROJCS["Madagascar Laborde (SEIMAD)",GEOGCS["Tananarive 1925",DATUM["Tananarive_1925",SPHEROID["International 1924",6378249.2,293.4660212936269],TOWGS84[-189,-242,-91,0,0,0,0]],PRIMEM["Greenwich",0],UNIT["degree",0.0174532925199433]],PROJECTION["Lambert_Conformal_Conic_2SP"],PARAMETER["latitude_of_origin",-18.96666666666667],PARAMETER["central_meridian",46.5],PARAMETER["standard_parallel_1",-18.96666666666667],PARAMETER["false_easting",400000],PARAMETER["false_northing",800000],UNIT["metre",1],AUTHORITY["SEIMAD","900914"]]',
  '+proj=lcc +lat_1=-18.96666666666667 +lat_0=-18.96666666666667 +lon_0=46.5 +k_0=0.9995 +x_0=400000 +y_0=800000 +ellps=intl +towgs84=-189,-242,-91,0,0,0,0 +units=m +no_defs'
)
ON CONFLICT (srid) DO NOTHING;
