package com.seimad.patrimoine.dto.carte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Géométrie de la carte interactive — projection native de la requête
 * {@code listerToutes} (GeometrieRepository), enrichie du libellé de
 * l'entité rattachée (nom de propriété, n° de dossier…).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeometrieCarteDTO {

    private String idGeometrie;

    /** 'parcelle', 'propriete', 'dossier_parcelle', 'signalement', 'descente_terrain'. */
    private String entiteType;

    private String entiteId;

    /** 'Point', 'Polygon' ou 'LineString'. */
    private String typeGeometrie;

    /** GeoJSON régénéré par PostGIS : {"type":"Point","coordinates":[lng,lat]}. */
    private String geojson;

    private BigDecimal precisionM;

    private BigDecimal superficieCalculeeM2;

    /** Source du relevé : "Tracé manuel", "Import GPS", "Saisie coordonnées"… */
    private String source;

    private LocalDateTime dateCreation;

    private LocalDateTime dateModification;

    /** Nom de l'utilisateur ayant créé la géométrie. */
    private String utilisateur;

    /** Libellé métier de l'entité (nom propriété, numéro de dossier…). */
    private String libelleEntite;

    /** Pour une parcelle de dossier : le dossier porteur est-il clôturé (statut final) ? */
    private Boolean dossierCloture;

    /** Pour une propriété : liée à un dossier via dossier_parcelle (couche orange). */
    private Boolean lieeADossier;
}
