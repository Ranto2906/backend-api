package com.seimad.patrimoine.dto.carte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Géométrie renvoyée au frontend — miroir lisible de la ligne `geometrie`
 * (PostGIS). Le champ `geojson` est régénéré depuis la colonne `geom`
 * (ST_AsGeoJSON) : {"type":"Point","coordinates":[lng,lat]}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeometrieDTO {

    private String idGeometrie;

    private String entiteType;

    private String entiteId;

    /** 'Point', 'Polygon' ou 'LineString'. */
    private String typeGeometrie;

    /** GeoJSON texte régénéré depuis PostGIS. */
    private String geojson;

    private BigDecimal precisionM;

    private BigDecimal superficieCalculeeM2;

    /** Source du relevé : "Import GPS", "Point carte", "Tracé manuel"… */
    private String source;

    private LocalDateTime dateCreation;

    private LocalDateTime dateModification;
}
