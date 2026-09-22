package com.seimad.patrimoine.dto.carte;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Demande d'enregistrement d'une géométrie depuis la carte interactive
 * (dessin manuel d'un terrain : mode Polygone ou Point, superficie
 * calculée côté client avec turf.js).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeometrieCreateRequest {

    /** 'Point', 'Polygon' ou 'LineString'. */
    @NotBlank(message = "Le type de géométrie est obligatoire")
    private String typeGeometrie;

    /** GeoJSON texte (ordre lng,lat). */
    @NotBlank(message = "La géométrie (GeoJSON) est obligatoire")
    private String geojson;

    /** Entité cible : 'parcelle', 'propriete' ou 'dossier_parcelle'. */
    @NotBlank(message = "L'entité cible est obligatoire")
    private String entiteType;

    /** ID de l'entité cible (UUID ; propriété → 00000000-0000-0000-0000-{id:012d}). */
    @NotBlank(message = "L'identifiant de l'entité cible est obligatoire")
    private String entiteId;

    /** Superficie calculée côté client (turf.js), en m² (optionnelle). */
    private Double superficieCalculeeM2;

    /** Remplacer la géométrie existante de l'entité (sinon erreur). */
    private Boolean remplacer;

    /** Source : "Tracé manuel" (défaut) ou "Saisie coordonnées". */
    private String source;
}
