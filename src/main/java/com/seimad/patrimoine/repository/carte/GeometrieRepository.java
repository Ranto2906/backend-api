package com.seimad.patrimoine.repository.carte;

import com.seimad.patrimoine.entity.carte.Geometrie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    @Modifying
    @Query(value = "DELETE FROM geometrie WHERE entite_type = :entiteType AND entite_id = :entiteId",
            nativeQuery = true)
    int supprimerParEntite(@Param("entiteType") String entiteType,
                           @Param("entiteId") UUID entiteId);
}
