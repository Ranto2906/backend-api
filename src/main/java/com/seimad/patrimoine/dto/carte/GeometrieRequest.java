package com.seimad.patrimoine.dto.carte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Géométrie attachée à une entité (ex. signalement) — reçue du mobile en
 * GeoJSON texte (ex. {"type":"Point","coordinates":[47.507,-18.879]}).
 * Stockée côté serveur dans la table `geometrie` (PostGIS, SRID 4326).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeometrieRequest {

    /** 'Point', 'Polygon' ou 'LineString' — cohérent avec le GeoJSON reçu. */
    private String typeGeometrie;

    /** GeoJSON texte : {"type":"Point","coordinates":[lng,lat]} (ordre GeoJSON). */
    private String geojson;

    /** Précision GPS en mètres (optionnelle). */
    private Double precisionM;

    /** Source du relevé (ex. "Point carte", "Import GPS", "Tracé manuel"). */
    private String source;
}
