package com.seimad.patrimoine.entity.carte;

import com.seimad.patrimoine.entity.auth.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Geometry;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Géométrie polymorphe : rattachée à parcelle, titre_foncier,
 * dossier_parcelle, signalement ou descente_terrain via (entite_type, entite_id).
 * Utilise PostGIS geography(Geometry, 4326) – coordonnées WGS84.
 */
@Entity
@Table(name = "geometrie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Geometrie {

    @Id
    @Column(name = "id_geometrie")
    private UUID idGeometrie;

    @Column(name = "entite_type", nullable = false, length = 30)
    private String entiteType;

    @Column(name = "entite_id", nullable = false)
    private UUID entiteId;

    @Column(name = "type_geometrie", nullable = false, length = 20)
    private String typeGeometrie;

    @Column(columnDefinition = "geography(Geometry, 4326)", nullable = false)
    @JdbcTypeCode(SqlTypes.GEOMETRY)
    private Geometry geom;

    @Column(name = "precision_m", precision = 8, scale = 2)
    private BigDecimal precisionM;

    @Column(name = "superficie_calculee_m2", precision = 12, scale = 3)
    private BigDecimal superficieCalculeeM2;

    @Column(name = "source", length = 30)
    @Builder.Default
    private String source = "Tracé manuel";

    @Column(name = "date_creation")
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_modification")
    @Builder.Default
    private LocalDateTime dateModification = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;
}
