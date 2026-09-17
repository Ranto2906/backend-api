package com.seimad.patrimoine.controller.descente;

import com.seimad.patrimoine.dto.descente.DescenteTerrainDTO;
import com.seimad.patrimoine.dto.descente.DescenteTerrainRequest;
import com.seimad.patrimoine.service.descente.DescenteTerrainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/descentes-terrain")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:8100"})
@RequiredArgsConstructor
@Slf4j
public class DescenteTerrainController {

    private final DescenteTerrainService descenteTerrainService;

    // ── GET /api/descentes-terrain ──
    @GetMapping
    public ResponseEntity<Page<DescenteTerrainDTO>> lister(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(descenteTerrainService.lister(PageRequest.of(page, size)));
    }

    // ── GET /api/descentes-terrain/search?search= ──
    @GetMapping("/search")
    public ResponseEntity<Page<DescenteTerrainDTO>> rechercher(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(descenteTerrainService.rechercher(search, PageRequest.of(page, size)));
    }

    // ── GET /api/descentes-terrain/{id} ──
    @GetMapping("/{id}")
    public ResponseEntity<DescenteTerrainDTO> trouverParId(@PathVariable UUID id) {
        return ResponseEntity.ok(descenteTerrainService.trouverParId(id));
    }

    // ── POST /api/descentes-terrain ──
    @PostMapping
    public ResponseEntity<DescenteTerrainDTO> creer(@Valid @RequestBody DescenteTerrainRequest request) {
        log.info("Création d'une descente terrain — date={}, statut={}, mode={}",
                request.getDateDescente(), request.getStatutConstat(), request.getMode());
        return ResponseEntity.status(HttpStatus.CREATED).body(descenteTerrainService.creer(request));
    }

    // ── PUT /api/descentes-terrain/{id} ──
    @PutMapping("/{id}")
    public ResponseEntity<DescenteTerrainDTO> mettreAJour(
            @PathVariable UUID id,
            @Valid @RequestBody DescenteTerrainRequest request) {
        log.info("Mise à jour descente terrain id={}", id);
        return ResponseEntity.ok(descenteTerrainService.mettreAJour(id, request));
    }

    // ── DELETE /api/descentes-terrain/{id} ──
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        log.info("Suppression descente terrain id={}", id);
        descenteTerrainService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    // ── GET /api/descentes-terrain/stats ──
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> statistiques() {
        return ResponseEntity.ok(descenteTerrainService.statistiques());
    }

    // ── GET /api/descentes-terrain/dossiers/search?q= ──
    // Recherche de dossiers pour l'autocomplete du mobile
    @GetMapping("/dossiers/search")
    public ResponseEntity<List<Map<String, Object>>> rechercherDossiers(
            @RequestParam(name = "q", required = false) String search) {
        return ResponseEntity.ok(descenteTerrainService.rechercherDossiers(search));
    }
}
