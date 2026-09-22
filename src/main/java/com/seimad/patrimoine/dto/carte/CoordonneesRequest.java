package com.seimad.patrimoine.dto.carte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Formulaire « Saisir des coordonnées » de la carte interactive.
 * <p>
 * Deux formats acceptés, détectés automatiquement :
 * <ul>
 *   <li>paires {@code lat,lon} ou {@code lon,lat} (WGS84), une par ligne ;</li>
 *   <li>paires {@code X Y} Laborde (grille malgache, conversion PostGIS).</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoordonneesRequest {

    /** 'Point', 'Polygon' ou 'LineString'. */
    private String typeGeometrie;

    /**
     * Liste de points. Point : une seule paire. Polygone : ordre du tracé
     * (fermeture automatique côté serveur). Ligne : ordre du tracé.
     */
    private List<PointSaisi> points;

    /** Entité cible : 'parcelle', 'propriete' ou 'dossier_parcelle'. */
    private String entiteType;

    /** ID de l'entité cible. */
    private String entiteId;

    /** true si les paires sont du Laborde X/Y (sinon WGS84 lat/lon). */
    private Boolean laborde;

    /** true si les paires WGS84 sont au format lon,lat (défaut lat,lon). */
    private Boolean lonLat;

    /** Remplacer la géométrie existante de l'entité (sinon erreur 409). */
    private Boolean remplacer;

    /** Source du relevé (défaut "Saisie coordonnées"). */
    private String source;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PointSaisi {
        private Double x;
        private Double y;
    }
}
