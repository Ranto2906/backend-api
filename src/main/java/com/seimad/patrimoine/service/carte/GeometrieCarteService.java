package com.seimad.patrimoine.service.carte;

import com.seimad.patrimoine.dto.carte.CoordonneesRequest;
import com.seimad.patrimoine.dto.carte.EntiteCibleCarteDTO;
import com.seimad.patrimoine.dto.carte.GeometrieCarteDTO;
import com.seimad.patrimoine.dto.carte.GeometrieCreateRequest;
import com.seimad.patrimoine.repository.auth.UtilisateurRepository;
import com.seimad.patrimoine.repository.carte.GeometrieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Logique de la carte interactive (module 4.6.1) :
 * <ul>
 *   <li>affichage de toutes les géométries de la table {@code geometrie}
 *       (points, polygones, lignes) avec libellé de l'entité rattachée ;</li>
 *   <li>enregistrement d'un <b>tracé manuel</b> (dessin sur la carte) —
 *       superficie calculée côté client via turf.js ;</li>
 *   <li><b>saisie de coordonnées</b> : paires lat,lon (WGS84) ou X/Y Laborde
 *       (grille malgache, SRID 900914 — conversion PostGIS ST_Transform),
 *       point / polygone / ligne, détection automatique du format ;</li>
 *   <li>entités cibles autorisées : parcelle, propriété, parcelle de dossier.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeometrieCarteService {

    private final GeometrieRepository geometrieRepository;
    private final UtilisateurRepository utilisateurRepository;

    /** Entités cibles autorisées pour un tracé manuel (module 4.6.1). */
    private static final Set<String> ENTITES_AUTORISEES = Set.of("parcelle", "propriete", "dossier_parcelle");

    /** Sources autorisées par la contrainte CHECK(source ...) de la table geometrie. */
    private static final Set<String> SOURCES_AUTORISEES = Set.of(
            "Tracé manuel", "Import GPS", "Calculé automatiquement", "Point carte",
            "Recherche de lieu", "Import Excel", "Saisie coordonnées");

    private static final String SOURCE_MANUELLE = "Tracé manuel";
    private static final String SOURCE_SAISIE = "Saisie coordonnées";

    // ─────────────────────────────────────────────────────────
    // Lecture
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GeometrieCarteDTO> listerToutes() {
        return geometrieRepository.listerToutes().stream()
                .map(GeometrieCarteService::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public GeometrieCarteDTO trouverParId(UUID id) {
        List<Map<String, Object>> lignes = geometrieRepository.projeterParId(id);
        if (lignes.isEmpty()) {
            throw new IllegalArgumentException("Géométrie introuvable : " + id);
        }
        return toDTO(lignes.get(0));
    }

    // ─────────────────────────────────────────────────────────
    // Entités ciblables (localiser / créer une propriété ou une parcelle)
    // ─────────────────────────────────────────────────────────

    /**
     * Entités que l'on peut placer sur la carte : propriétés ou parcelles, avec
     * un filtre « sans géométrie » par défaut (une entité déjà localisée remonte
     * seulement si {@code inclureLocalisees} est vrai, et son drapeau le signale).
     */
    @Transactional(readOnly = true)
    public List<EntiteCibleCarteDTO> listerEntitesCibles(String type, String recherche,
                                                         boolean inclureLocalisees, int limite) {
        String t = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        String q = recherche == null ? "" : recherche.trim();
        int max = limite <= 0 ? 40 : Math.min(limite, 200);

        // Filtre textuel ('toutes' / 'sans') plutôt qu'un booléen : le binding est
        // sans ambiguïté dans une projection native PostgreSQL.
        String filtre = inclureLocalisees ? "toutes" : "sans";
        List<Map<String, Object>> lignes = switch (t) {
            case "propriete" -> geometrieRepository.entitesCiblesPropriete(q, filtre, max);
            case "parcelle" -> geometrieRepository.entitesCiblesParcelle(q, filtre, max);
            default -> throw new IllegalArgumentException(
                    "Entité ciblable invalide : propriete ou parcelle attendu");
        };
        return lignes.stream().map(GeometrieCarteService::toEntiteDTO).toList();
    }

    // ─────────────────────────────────────────────────────────
    // Tracé manuel (dessin sur la carte)
    // ─────────────────────────────────────────────────────────

    @Transactional
    public GeometrieCarteDTO enregistrer(GeometrieCreateRequest request) {
        String entiteType = validerEntite(request.getEntiteType());
        UUID entiteId = convertirEntiteId(entiteType, request.getEntiteId());

        String type = normaliserType(request.getTypeGeometrie());
        String geojson = validerGeojson(request.getGeojson(), type);
        String source = normaliserSource(request.getSource(), SOURCE_MANUELLE);

        if (geometrieRepository.existePourEntite(entiteType, entiteId)
                && !Boolean.TRUE.equals(request.getRemplacer())) {
            throw new IllegalStateException(
                    "Cette entité possède déjà une géométrie — cochez « Remplacer » pour la remplacer");
        }

        BigDecimal superficie = request.getSuperficieCalculeeM2() != null
                ? BigDecimal.valueOf(request.getSuperficieCalculeeM2()).setScale(3, RoundingMode.HALF_UP)
                : null;

        geometrieRepository.upsertAvecSuperficie(entiteType, entiteId, type, geojson, null, superficie,
                source, idUtilisateurConnecte());

        log.info("Carte — tracé manuel enregistré : {} {} ({} {})", entiteType, entiteId, type, source);
        return trouverParEntite(entiteType, entiteId);
    }

    // ─────────────────────────────────────────────────────────
    // Saisie de coordonnées (Laborde ou WGS84)
    // ─────────────────────────────────────────────────────────

    @Transactional
    public GeometrieCarteDTO enregistrerCoordonnees(CoordonneesRequest request) {
        String entiteType = validerEntite(request.getEntiteType());
        UUID entiteId = convertirEntiteId(entiteType, request.getEntiteId());

        String type = normaliserType(request.getTypeGeometrie());
        if (request.getPoints() == null || request.getPoints().isEmpty()) {
            throw new IllegalArgumentException("Aucune coordonnée fournie");
        }
        if ("Point".equals(type) && request.getPoints().size() != 1) {
            throw new IllegalArgumentException("Un point nécessite une seule paire de coordonnées");
        }
        if (!"Point".equals(type) && request.getPoints().size() < 3) {
            throw new IllegalArgumentException("Un polygone ou une ligne nécessite au moins 3 points");
        }
        if (!geometrieRepository.sridLabordeDisponible()) {
            throw new IllegalStateException(
                    "SRID Laborde (900914) indisponible dans PostGIS : la conversion X/Y est impossible");
        }
        if (geometrieRepository.existePourEntite(entiteType, entiteId)
                && !Boolean.TRUE.equals(request.getRemplacer())) {
            throw new IllegalStateException(
                    "Cette entité possède déjà une géométrie — cochez « Remplacer » pour la remplacer");
        }

        String source = normaliserSource(request.getSource(), SOURCE_SAISIE);
        if (Boolean.TRUE.equals(request.getLaborde())) {
            String geojson = geojsonDepuisLaborde(request.getPoints(), type);
            geometrieRepository.upsertDepuisLaborde(entiteType, entiteId, type, geojson, source,
                    idUtilisateurConnecte());
        } else {
            String geojson = geojsonDepuisWgs84(request.getPoints(), type,
                    Boolean.TRUE.equals(request.getLonLat()));
            geometrieRepository.upsertAvecSuperficie(entiteType, entiteId, type, geojson, null, null,
                    source, idUtilisateurConnecte());
        }

        log.info("Carte — géométrie créée depuis coordonnées : {} {} ({})", entiteType, entiteId, type);
        return trouverParEntite(entiteType, entiteId);
    }

    /** Construit le GeoJSON et laisse PostGIS convertir (ST_Transform 900914 → 4326). */
    private String geojsonDepuisLaborde(List<CoordonneesRequest.PointSaisi> points, String type) {
        StringBuilder coords = new StringBuilder();
        for (CoordonneesRequest.PointSaisi p : points) {
            if (p == null || p.getX() == null || p.getY() == null) {
                throw new IllegalArgumentException("Paire X/Y Laborde invalide");
            }
            if (coords.length() > 0) coords.append(',');
            coords.append('[').append(p.getX()).append(',').append(p.getY()).append(']');
        }
        // Squelette GeoJSON en Laborde : ST_Transform accepte un JSON brut
        // (ST_GeomFromGeoJSON renvoie du SRID 0 que l'on requalifie en 900914).
        return switch (type) {
            case "Point" -> "{\"type\":\"Point\",\"coordinates\":" + coords + "}";
            case "LineString" -> "{\"type\":\"LineString\",\"coordinates\":[" + coords + "]}";
            default -> "{\"type\":\"Polygon\",\"coordinates\":[[" + coords
                    + (coords.length() > 0 ? "," : "") + premierPoint(points) + "]]}";
        };
    }

    private String premierPoint(List<CoordonneesRequest.PointSaisi> points) {
        if (points.isEmpty() || points.get(0) == null
                || points.get(0).getX() == null || points.get(0).getY() == null) {
            throw new IllegalArgumentException("Paire X/Y Laborde invalide");
        }
        return "[" + points.get(0).getX() + "," + points.get(0).getY() + "]";
    }

    private String geojsonDepuisWgs84(List<CoordonneesRequest.PointSaisi> points, String type, boolean lonLat) {
        StringBuilder coords = new StringBuilder();
        for (CoordonneesRequest.PointSaisi p : points) {
            if (p == null || p.getX() == null || p.getY() == null) {
                throw new IllegalArgumentException("Paire de coordonnées invalide");
            }
            if (coords.length() > 0) coords.append(',');
            if (lonLat) {
                coords.append('[').append(p.getX()).append(',').append(p.getY()).append(']');
            } else {
                coords.append('[').append(p.getY()).append(',').append(p.getX()).append(']');
            }
        }
        return switch (type) {
            case "Point" -> "{\"type\":\"Point\",\"coordinates\":" + coords + "}";
            case "LineString" -> "{\"type\":\"LineString\",\"coordinates\":[" + coords + "]}";
            default -> "{\"type\":\"Polygon\",\"coordinates\":[[" + coords
                    + (coords.length() > 0 ? "," : "") + premierPointWgs84(points, lonLat) + "]]}";
        };
    }

    private String premierPointWgs84(List<CoordonneesRequest.PointSaisi> points, boolean lonLat) {
        if (points.isEmpty() || points.get(0) == null
                || points.get(0).getX() == null || points.get(0).getY() == null) {
            throw new IllegalArgumentException("Paire de coordonnées invalide");
        }
        return lonLat
                ? "[" + points.get(0).getX() + "," + points.get(0).getY() + "]"
                : "[" + points.get(0).getY() + "," + points.get(0).getX() + "]";
    }

    // ─────────────────────────────────────────────────────────
    // Validations
    // ─────────────────────────────────────────────────────────

    private String validerEntite(String entiteType) {
        String type = entiteType == null ? "" : entiteType.trim().toLowerCase(Locale.ROOT);
        if (!ENTITES_AUTORISEES.contains(type)) {
            throw new IllegalArgumentException(
                    "Entité cible invalide : parcelle, propriete ou dossier_parcelle attendu");
        }
        return type;
    }

    private String normaliserType(String typeGeometrie) {
        String t = typeGeometrie == null ? "" : typeGeometrie.trim();
        if (t.equalsIgnoreCase("polygon") || t.equalsIgnoreCase("polygone")) return "Polygon";
        if (t.equalsIgnoreCase("linestring") || t.equalsIgnoreCase("ligne")) return "LineString";
        if (t.equalsIgnoreCase("point")) return "Point";
        throw new IllegalArgumentException("Type de géométrie invalide : " + typeGeometrie);
    }

    private String normaliserSource(String source, String defaut) {
        if (source == null || source.isBlank()) return defaut;
        if (SOURCES_AUTORISEES.contains(source)) return source;
        throw new IllegalArgumentException("Source non autorisée : " + source);
    }

    private String validerGeojson(String geojson, String type) {
        if (geojson == null || geojson.isBlank()) {
            throw new IllegalArgumentException("GeoJSON vide");
        }
        return geojson.trim();
    }

    private UUID convertirEntiteId(String entiteType, String entiteId) {
        if (entiteId == null || entiteId.isBlank()) {
            throw new IllegalArgumentException("Identifiant de l'entité cible manquant");
        }
        try {
            if ("propriete".equals(entiteType)) {
                // Convention : 00000000-0000-0000-0000-{id:012d}
                try {
                    UUID uuid = UUID.fromString(entiteId.trim());
                    if (uuid.toString().matches("00000000-0000-0000-0000-\\d{12}")) return uuid;
                } catch (IllegalArgumentException ignored) {
                    // pas un UUID : entier brut → conversion
                }
                int id = Integer.parseInt(entiteId.trim());
                return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", id));
            }
            return UUID.fromString(entiteId.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Identifiant d'entité invalide : " + entiteId);
        }
    }

    /** ID de l'utilisateur authentifié (obligatoire : colonne NOT NULL en base). */
    private Integer idUtilisateurConnecte() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getPrincipal()) || !auth.isAuthenticated()) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return utilisateurRepository.findByNomUtilisateur(auth.getName())
                .map(u -> u.getIdUtilisateur())
                .orElseThrow(() -> new IllegalStateException("Utilisateur inconnu : " + auth.getName()));
    }

    private GeometrieCarteDTO trouverParEntite(String entiteType, UUID entiteId) {
        List<Map<String, Object>> lignes = geometrieRepository.projeterParEntite(entiteType, entiteId);
        if (lignes.isEmpty()) {
            throw new IllegalStateException("Géométrie introuvable après enregistrement");
        }
        return toDTO(lignes.get(0));
    }

    private static GeometrieCarteDTO toDTO(Map<String, Object> ligne) {
        return GeometrieCarteDTO.builder()
                .idGeometrie(chaine(ligne, "idGeometrie"))
                .entiteType(chaine(ligne, "entiteType"))
                .entiteId(chaine(ligne, "entiteId"))
                .typeGeometrie(chaine(ligne, "typeGeometrie"))
                .geojson(chaine(ligne, "geojson"))
                .precisionM(versBigDecimal(valeur(ligne, "precisionM")))
                .superficieCalculeeM2(versBigDecimal(valeur(ligne, "superficieCalculeeM2")))
                .source(chaine(ligne, "source"))
                .dateCreation(horodatage(ligne, "dateCreation"))
                .dateModification(horodatage(ligne, "dateModification"))
                .utilisateur(chaine(ligne, "utilisateur"))
                .libelleEntite(chaine(ligne, "libelleEntite"))
                .dossierCloture(booleen(ligne, "dossierCloture"))
                .lieeADossier(booleen(ligne, "lieeADossier"))
                .build();
    }

    private static EntiteCibleCarteDTO toEntiteDTO(Map<String, Object> ligne) {
        return EntiteCibleCarteDTO.builder()
                .id(chaine(ligne, "id"))
                .libelle(chaine(ligne, "libelle"))
                .details(chaine(ligne, "details"))
                .type(chaine(ligne, "entiteType"))
                .localisee(booleen(ligne, "localisee"))
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // Lecture tolérante des projections natives
    // ─────────────────────────────────────────────────────────

    /**
     * PostgreSQL abaisse les alias non cités : la casse des colonnes renvoyées
     * par le driver peut donc différer de celle écrite dans la requête. On tente
     * la clé exacte puis une comparaison insensible à la casse.
     */
    private static Object valeur(Map<String, Object> ligne, String cle) {
        Object direct = ligne.get(cle);
        if (direct != null) return direct;
        for (Map.Entry<String, Object> e : ligne.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(cle)) return e.getValue();
        }
        return null;
    }

    private static String chaine(Map<String, Object> ligne, String cle) {
        Object v = valeur(ligne, cle);
        return v != null ? v.toString() : null;
    }

    private static Boolean booleen(Map<String, Object> ligne, String cle) {
        Object v = valeur(ligne, cle);
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        return Boolean.TRUE.toString().equalsIgnoreCase(v.toString());
    }

    private static BigDecimal versBigDecimal(Object v) {
        return v != null ? new BigDecimal(v.toString()) : null;
    }

    /** Convertit un timestamp JDBC (ou un LocalDateTime déjà mappé) en LocalDateTime. */
    private static java.time.LocalDateTime horodatage(Map<String, Object> ligne, String cle) {
        Object v = valeur(ligne, cle);
        if (v == null) return null;
        if (v instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        if (v instanceof java.time.LocalDateTime ldt) return ldt;
        if (v instanceof java.time.OffsetDateTime odt) return odt.toLocalDateTime();
        if (v instanceof java.time.Instant i) {
            return java.time.LocalDateTime.ofInstant(i, java.time.ZoneId.systemDefault());
        }
        return null;
    }
}
