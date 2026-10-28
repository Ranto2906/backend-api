package com.seimad.patrimoine.controller.descente;

import com.seimad.patrimoine.dto.descente.DescenteStatutHistoriqueDTO;
import com.seimad.patrimoine.dto.descente.DescenteTerrainDTO;
import com.seimad.patrimoine.dto.descente.DescenteTerrainRequest;
import com.seimad.patrimoine.service.descente.DescenteTerrainPdfService;
import com.seimad.patrimoine.service.descente.DescenteTerrainService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final DescenteTerrainPdfService descenteTerrainPdfService;

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

    // ── GET /api/descentes-terrain/{id}/historique-statuts — historique des
    // changements de statut (constat + validation) : date de modification, statut ──
    @GetMapping("/{id}/historique-statuts")
    @Operation(summary = "Historique des changements de statut d'une descente",
            description = "Reconstitué du journal d'audit : date de modification, ancien et nouveau statut, auteur")
    public ResponseEntity<List<DescenteStatutHistoriqueDTO>> historiqueStatuts(@PathVariable UUID id) {
        return ResponseEntity.ok(descenteTerrainService.historiqueStatuts(id));
    }

    // ── GET /api/descentes-terrain/{id}/pdf — fiche de descente terrain ──
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exporterPdf(@PathVariable UUID id) {
        DescenteTerrainDTO descente = descenteTerrainService.trouverParId(id);

        String generePar = "inconnu";
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            generePar = auth.getName();
        }

        byte[] pdf = descenteTerrainPdfService.genererPdf(descente, generePar);
        String reference = descente.getReference() == null ? String.valueOf(id)
                : descente.getReference().replaceAll("[^A-Za-z0-9._-]", "_");
        log.info("Export PDF de la descente {} ({} octets)", descente.getReference(), pdf.length);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"descente-" + reference + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
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
