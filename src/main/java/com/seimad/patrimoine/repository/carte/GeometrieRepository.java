package com.seimad.patrimoine.repository.carte;

import com.seimad.patrimoine.entity.carte.Geometrie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public interface GeometrieRepository extends JpaRepository<Geometrie, UUID> {

    /**
     * Upsert d'une géométrie rattachée à une entité (ex. signalement).
     * Le GeoJSON (texte) est converti en PostGIS geography(Geometry, 4326)
     * côté base via ST_GeomFromGeoJSON — aucune conversion JTS en Java.
     * UNIQUE(entite_type, entite_id) : une seule géométrie par entité.
     */
    @Modifying
    @Query(value = """
            INSERT INTO geometrie
                (id_geometrie, entite_type, entite_id, type_geometrie, geom,
                 precision_m, source, date_creation, date_modification, id_utilisateur)
            VALUES
                (gen_random_uuid(), :entiteType, :entiteId, :typeGeometrie,
                 ST_GeomFromGeoJSON(:geojson)::geography, :precisionM, :source,
                 now(), now(), :idUtilisateur)
            ON CONFLICT (entite_type, entite_id) DO UPDATE SET
                type_geometrie = EXCLUDED.type_geometrie,
                geom          = EXCLUDED.geom,
                precision_m   = EXCLUDED.precision_m,
                superficie_calculee_m2 = EXCLUDED.superficie_calculee_m2,
                source        = EXCLUDED.source,
                date_modification = now()
            """, nativeQuery = true)
    int upsert(@Param("entiteType") String entiteType,
               @Param("entiteId") UUID entiteId,
               @Param("typeGeometrie") String typeGeometrie,
               @Param("geojson") String geojson,
               @Param("precisionM") BigDecimal precisionM,
               @Param("source") String source,
               @Param("idUtilisateur") Integer idUtilisateur);

    /**
     * Variante avec superficie calculée côté client (turf.js) — utilisée par
     * la carte interactive ; met aussi à jour superficie_calculee_m2.
     */
    @Modifying
    @Query(value = """
            INSERT INTO geometrie
                (id_geometrie, entite_type, entite_id, type_geometrie, geom,
                 precision_m, superficie_calculee_m2, source,
                 date_creation, date_modification, id_utilisateur)
            VALUES
                (gen_random_uuid(), :entiteType, :entiteId, :typeGeometrie,
                 ST_GeomFromGeoJSON(:geojson)::geography, :precisionM, :superficie,
                 :source, now(), now(), :idUtilisateur)
            ON CONFLICT (entite_type, entite_id) DO UPDATE SET
                type_geometrie = EXCLUDED.type_geometrie,
                geom          = EXCLUDED.geom,
                precision_m   = EXCLUDED.precision_m,
                superficie_calculee_m2 = EXCLUDED.superficie_calculee_m2,
                source        = EXCLUDED.source,
                date_modification = now()
            """, nativeQuery = true)
    int upsertAvecSuperficie(@Param("entiteType") String entiteType,
                             @Param("entiteId") UUID entiteId,
                             @Param("typeGeometrie") String typeGeometrie,
                             @Param("geojson") String geojson,
                             @Param("precisionM") BigDecimal precisionM,
                             @Param("superficie") BigDecimal superficie,
                             @Param("source") String source,
                             @Param("idUtilisateur") Integer idUtilisateur);

    @Modifying
    @Query(value = "DELETE FROM geometrie WHERE entite_type = :entiteType AND entite_id = :entiteId",
            nativeQuery = true)
    int supprimerParEntite(@Param("entiteType") String entiteType,
                           @Param("entiteId") UUID entiteId);

    /**
     * Point depuis des coordonnées X/Y en grille Laborde (SRID 900914, mètres —
     * définition enregistrée par la migration V20, cf. sa remarque sur EPSG:29701).
     */
    @Modifying
    @Query(value = """
            INSERT INTO geometrie
                (id_geometrie, entite_type, entite_id, type_geometrie, geom,
                 precision_m, source, date_creation, date_modification, id_utilisateur)
            VALUES
                (gen_random_uuid(), :entiteType, :entiteId, 'Point',
                 ST_Transform(ST_SetSRID(ST_MakePoint(:xLaborde, :yLaborde), 900914), 4326)::geography,
                 50, :source, now(), now(), :idUtilisateur)
            ON CONFLICT (entite_type, entite_id) DO UPDATE SET
                geom          = EXCLUDED.geom,
                source        = EXCLUDED.source,
                date_modification = now()
            """, nativeQuery = true)
    int upsertPointLaborde(@Param("entiteType") String entiteType,
                           @Param("entiteId") UUID entiteId,
                           @Param("xLaborde") double xLaborde,
                           @Param("yLaborde") double yLaborde,
                           @Param("source") String source,
                           @Param("idUtilisateur") Integer idUtilisateur);

    /**
     * Upsert d'une géométrie (Point, Polygon ou LineString) saisie en grille
     * Laborde : le GeoJSON contient les X/Y bruts (mètres) ; PostGIS le
     * requalifie en SRID 900914 (V20) puis le convertit en WGS84.
     */
    @Modifying
    @Query(value = """
            INSERT INTO geometrie
                (id_geometrie, entite_type, entite_id, type_geometrie, geom,
                 precision_m, source, date_creation, date_modification, id_utilisateur)
            VALUES
                (gen_random_uuid(), :entiteType, :entiteId, :typeGeometrie,
                 ST_Transform(ST_SetSRID(ST_GeomFromGeoJSON(:geojson), 900914), 4326)::geography,
                 50, :source, now(), now(), :idUtilisateur)
            ON CONFLICT (entite_type, entite_id) DO UPDATE SET
                type_geometrie = EXCLUDED.type_geometrie,
                geom          = EXCLUDED.geom,
                source        = EXCLUDED.source,
                date_modification = now()
            """, nativeQuery = true)
    int upsertDepuisLaborde(@Param("entiteType") String entiteType,
                            @Param("entiteId") UUID entiteId,
                            @Param("typeGeometrie") String typeGeometrie,
                            @Param("geojson") String geojson,
                            @Param("source") String source,
                            @Param("idUtilisateur") Integer idUtilisateur);

    /**
     * Propriétés ciblables depuis la carte : id entier, nom, complément lisible
     * (numéro · zone · lieu · ville) et drapeau « déjà localisée ». Les entités
     * sans géométrie remontent en tête — ce sont celles à placer sur la carte.
     */
    @Query(value = """
            SELECT p.id_propriete::text AS id,
                   p.nom                AS libelle,
                   NULLIF(concat_ws(' · ',
                       NULLIF(p.numero, ''),
                       NULLIF(p.zone, ''),
                       NULLIF(l.libelle, ''),
                       NULLIF(v.nom_ville, '')), '') AS details,
                   'propriete' AS entiteType,
                   (g.id_geometrie IS NOT NULL) AS localisee
            FROM propriete p
            LEFT JOIN lieu  l ON l.id_lieu = p.id_lieu
            LEFT JOIN ville v ON v.id_ville = l.id_ville
            LEFT JOIN geometrie g
                   ON g.entite_type = 'propriete'
                  AND g.entite_id = ('00000000-0000-0000-0000-' ||
                        lpad(p.id_propriete::text, 12, '0'))::uuid
            WHERE (:filtreGeometrie = 'toutes' OR g.id_geometrie IS NULL)
              AND (:recherche = ''
                   OR p.nom ILIKE '%' || :recherche || '%'
                   OR coalesce(p.numero, '') ILIKE '%' || :recherche || '%'
                   OR coalesce(l.libelle, '') ILIKE '%' || :recherche || '%')
            ORDER BY (g.id_geometrie IS NOT NULL), p.nom
            LIMIT :limite
            """, nativeQuery = true)
    List<Map<String, Object>> entitesCiblesPropriete(@Param("recherche") String recherche,
                                                     @Param("filtreGeometrie") String filtreGeometrie,
                                                     @Param("limite") int limite);

    /**
     * Parcelles ciblables depuis la carte : n° de lot, propriété rattachée,
     * lieu/ville, et drapeau « déjà localisée ».
     */
    @Query(value = """
            SELECT pa.id_parcelle::text AS id,
                   coalesce(pa.numero_lot,
                            'Parcelle ' || left(pa.id_parcelle::text, 8)) AS libelle,
                   NULLIF(concat_ws(' · ',
                       'Lot ' || pa.numero_lot,
                       pr.nom,
                       NULLIF(l.libelle, ''),
                       NULLIF(v.nom_ville, '')), '') AS details,
                   'parcelle' AS entiteType,
                   (g.id_geometrie IS NOT NULL) AS localisee
            FROM parcelle pa
            LEFT JOIN propriete pr ON pr.id_propriete = pa.id_propriete
            LEFT JOIN lieu  l ON l.id_lieu = pr.id_lieu
            LEFT JOIN ville v ON v.id_ville = l.id_ville
            LEFT JOIN geometrie g
                   ON g.entite_type = 'parcelle'
                  AND g.entite_id = pa.id_parcelle
            WHERE (:filtreGeometrie = 'toutes' OR g.id_geometrie IS NULL)
              AND (:recherche = ''
                   OR coalesce(pa.numero_lot, '') ILIKE '%' || :recherche || '%'
                   OR coalesce(pr.nom, '') ILIKE '%' || :recherche || '%')
            ORDER BY (g.id_geometrie IS NOT NULL), pr.nom, pa.numero_lot
            LIMIT :limite
            """, nativeQuery = true)
    List<Map<String, Object>> entitesCiblesParcelle(@Param("recherche") String recherche,
                                                    @Param("filtreGeometrie") String filtreGeometrie,
                                                    @Param("limite") int limite);

    /** Vérifie que le SRID Laborde (900914, migration V20) est connu de la base. */
    @Query(value = "SELECT COUNT(*) > 0 FROM spatial_ref_sys WHERE srid = 900914", nativeQuery = true)
    boolean sridLabordeDisponible();

    /**
     * Toutes les géométries de la carte interactive, enrichies du libellé
     * métier de l'entité rattachée. Codage couleur côté frontend par
     * entite_type (propriétés bleu, dossiers clôturés rouge, parcelles vert).
     */
    @Query(value = """
            SELECT g.id_geometrie::text       AS idGeometrie,
                   g.entite_type              AS entiteType,
                   g.entite_id::text          AS entiteId,
                   g.type_geometrie           AS typeGeometrie,
                   ST_AsGeoJSON(g.geom::geometry) AS geojson,
                   g.precision_m              AS precisionM,
                   g.superficie_calculee_m2   AS superficieCalculeeM2,
                   g.source                   AS source,
                   g.date_creation            AS dateCreation,
                   g.date_modification        AS dateModification,
                   u.nom_utilisateur          AS utilisateur,
                   CASE g.entite_type
                       WHEN 'propriete' THEN
                           (SELECT p.nom FROM propriete p
                            WHERE g.entite_id = ('00000000-0000-0000-0000-' ||
                                  lpad(p.id_propriete::text, 12, '0'))::uuid)
                       WHEN 'parcelle' THEN
                           (SELECT pa.numero_lot FROM parcelle pa WHERE pa.id_parcelle = g.entite_id)
                       WHEN 'dossier_parcelle' THEN
                           (SELECT d.numero_dossier FROM dossier_parcelle dp
                            JOIN dossier d ON d.id_dossier = dp.id_dossier
                            WHERE dp.id_dossier_parcelle = g.entite_id)
                       WHEN 'signalement' THEN
                           (SELECT s.reference FROM signalement s WHERE s.id_signalement = g.entite_id)
                       WHEN 'descente_terrain' THEN
                           (SELECT dt.reference FROM descente_terrain dt WHERE dt.id_descente = g.entite_id)
                   END AS libelleEntite,
                   CASE WHEN g.entite_type = 'dossier_parcelle' THEN
                       (SELECT sd.est_final
                        FROM dossier_parcelle dp
                        JOIN dossier d ON d.id_dossier = dp.id_dossier
                        LEFT JOIN statut_dossier sd ON sd.id_statut_dossier = d.id_statut_dossier
                        WHERE dp.id_dossier_parcelle = g.entite_id)
                   END AS dossierCloture,
                   CASE WHEN g.entite_type = 'propriete' THEN
                       EXISTS (SELECT 1 FROM dossier_parcelle dp
                               JOIN propriete p ON p.id_propriete = dp.id_propriete
                               WHERE g.entite_id = ('00000000-0000-0000-0000-' ||
                                     lpad(p.id_propriete::text, 12, '0'))::uuid)
                   END AS lieeADossier
            FROM geometrie g
            LEFT JOIN utilisateur u ON u.id_utilisateur = g.id_utilisateur
            ORDER BY g.date_modification DESC
            """, nativeQuery = true)
    List<Map<String, Object>> listerToutes();

    /** Même projection, restreinte à une géométrie (détail). */
    @Query(value = """
            SELECT g.id_geometrie::text       AS idGeometrie,
                   g.entite_type              AS entiteType,
                   g.entite_id::text          AS entiteId,
                   g.type_geometrie           AS typeGeometrie,
                   ST_AsGeoJSON(g.geom::geometry) AS geojson,
                   g.precision_m              AS precisionM,
                   g.superficie_calculee_m2   AS superficieCalculeeM2,
                   g.source                   AS source,
                   g.date_creation            AS dateCreation,
                   g.date_modification        AS dateModification,
                   u.nom_utilisateur          AS utilisateur,
                   CASE g.entite_type
                       WHEN 'propriete' THEN
                           (SELECT p.nom FROM propriete p
                            WHERE g.entite_id = ('00000000-0000-0000-0000-' ||
                                  lpad(p.id_propriete::text, 12, '0'))::uuid)
                       WHEN 'parcelle' THEN
                           (SELECT pa.numero_lot FROM parcelle pa WHERE pa.id_parcelle = g.entite_id)
                       WHEN 'dossier_parcelle' THEN
                           (SELECT d.numero_dossier FROM dossier_parcelle dp
                            JOIN dossier d ON d.id_dossier = dp.id_dossier
                            WHERE dp.id_dossier_parcelle = g.entite_id)
                       WHEN 'signalement' THEN
                           (SELECT s.reference FROM signalement s WHERE s.id_signalement = g.entite_id)
                       WHEN 'descente_terrain' THEN
                           (SELECT dt.reference FROM descente_terrain dt WHERE dt.id_descente = g.entite_id)
                   END AS libelleEntite,
                   CASE WHEN g.entite_type = 'dossier_parcelle' THEN
                       (SELECT sd.est_final
                        FROM dossier_parcelle dp
                        JOIN dossier d ON d.id_dossier = dp.id_dossier
                        LEFT JOIN statut_dossier sd ON sd.id_statut_dossier = d.id_statut_dossier
                        WHERE dp.id_dossier_parcelle = g.entite_id)
                   END AS dossierCloture,
                   CASE WHEN g.entite_type = 'propriete' THEN
                       EXISTS (SELECT 1 FROM dossier_parcelle dp
                               JOIN propriete p ON p.id_propriete = dp.id_propriete
                               WHERE g.entite_id = ('00000000-0000-0000-0000-' ||
                                     lpad(p.id_propriete::text, 12, '0'))::uuid)
                   END AS lieeADossier
            FROM geometrie g
            LEFT JOIN utilisateur u ON u.id_utilisateur = g.id_utilisateur
            WHERE g.id_geometrie = :id
            """, nativeQuery = true)
    List<Map<String, Object>> projeterParId(@Param("id") UUID id);

    /** Vrai si l'entité cible possède déjà une géométrie (contrôle 409). */
    @Query(value = "SELECT COUNT(*) > 0 FROM geometrie WHERE entite_type = :entiteType AND entite_id = :entiteId",
            nativeQuery = true)
    boolean existePourEntite(@Param("entiteType") String entiteType, @Param("entiteId") UUID entiteId);

    /**
     * Lecture d'une géométrie par entité — renvoie le GeoJSON régénéré depuis
     * PostGIS (ST_AsGeoJSON) avec les métadonnées (précision, source, dates).
     * UNIQUE(entite_type, entite_id) → au plus une ligne.
     */
    @Query(value = """
            SELECT id_geometrie::text        AS idGeometrie,
                   entite_type               AS entiteType,
                   entite_id::text           AS entiteId,
                   type_geometrie            AS typeGeometrie,
                   ST_AsGeoJSON(geom::geometry) AS geojson,
                   precision_m               AS precisionM,
                   superficie_calculee_m2    AS superficieCalculeeM2,
                   source                    AS source,
                   date_creation             AS dateCreation,
                   date_modification         AS dateModification
            FROM geometrie
            WHERE entite_type = :entiteType AND entite_id = :entiteId
            """, nativeQuery = true)
    java.util.List<Map<String, Object>> projeterParEntite(@Param("entiteType") String entiteType,
                                                          @Param("entiteId") UUID entiteId);
}
