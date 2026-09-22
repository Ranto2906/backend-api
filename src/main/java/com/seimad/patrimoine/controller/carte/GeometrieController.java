package com.seimad.patrimoine.controller.carte;

import com.seimad.patrimoine.dto.carte.CoordonneesRequest;
import com.seimad.patrimoine.dto.carte.EntiteCibleCarteDTO;
import com.seimad.patrimoine.dto.carte.GeometrieCarteDTO;
import com.seimad.patrimoine.dto.carte.GeometrieCreateRequest;
import com.seimad.patrimoine.service.carte.GeometrieCarteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * API de la carte interactive (module 4.6.1) :
 * <ul>
 *   <li>GET  /api/geometries             — toutes les géométries + libellés ;</li>
 *   <li>GET  /api/geometries/entites-cibles — propriétés / parcelles à
 *       localiser (sans géométrie par défaut) ;</li>
 *   <li>GET  /api/geometries/{id}        — détail d'une géométrie ;</li>
 *   <li>POST /api/geometries             — enregistrement d'un tracé manuel ;</li>
 *   <li>POST /api/geometries/coordonnees — saisie de coordonnées (Laborde ou
 *       lat/lon, point / polygone / ligne avec détection automatique).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/geometries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Carte interactive", description = "Géométries PostGIS affichées sur la carte interactive")
public class GeometrieController {

    private final GeometrieCarteService geometrieCarteService;

    @GetMapping
    @Operation(summary = "Toutes les géométries (points, polygones, lignes) avec libellé d'entité")
    public ResponseEntity<List<GeometrieCarteDTO>> lister() {
        return ResponseEntity.ok(geometrieCarteService.listerToutes());
    }

    @GetMapping("/entites-cibles")
    @Operation(summary = "Entités à localiser sur la carte (propriétés / parcelles, sans géométrie par défaut)")
    public ResponseEntity<List<EntiteCibleCarteDTO>> entitesCibles(
            @RequestParam(defaultValue = "propriete") String type,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean inclureLocalisees,
            @RequestParam(defaultValue = "40") int limite) {
        return ResponseEntity.ok(
                geometrieCarteService.listerEntitesCibles(type, search, inclureLocalisees, limite));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une géométrie")
    public ResponseEntity<GeometrieCarteDTO> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(geometrieCarteService.trouverParId(id));
    }

    @PostMapping
    @Operation(summary = "Enregistrer un tracé manuel (dessin sur la carte)")
    public ResponseEntity<GeometrieCarteDTO> creer(@RequestBody GeometrieCreateRequest request) {
        return ResponseEntity.ok(geometrieCarteService.enregistrer(request));
    }

    @PostMapping("/coordonnees")
    @Operation(summary = "Saisir des coordonnées (Laborde X/Y ou lat,lon) et créer la géométrie")
    public ResponseEntity<GeometrieCarteDTO> coordonnees(@RequestBody CoordonneesRequest request) {
        return ResponseEntity.ok(geometrieCarteService.enregistrerCoordonnees(request));
    }
}
