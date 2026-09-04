package com.seimad.patrimoine.controller.notification;

import com.seimad.patrimoine.dto.dossier.AuditDTO;
import com.seimad.patrimoine.dto.notification.AvertissementDTO;
import com.seimad.patrimoine.dto.notification.AvertissementRequest;
import com.seimad.patrimoine.service.dossier.AuditService;
import com.seimad.patrimoine.service.notification.AvertissementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/avertissements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Avertissements", description = "CRUD avertissements et alertes")
public class AvertissementController {

    private final AvertissementService avertissementService;
    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Lister les avertissements (pagination)")
    public ResponseEntity<Page<AvertissementDTO>> lister(Pageable pageable) {
        return ResponseEntity.ok(avertissementService.lister(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des avertissements")
    public ResponseEntity<Page<AvertissementDTO>> rechercher(
            @RequestParam(defaultValue = "") String search,
            Pageable pageable) {
        return ResponseEntity.ok(avertissementService.rechercher(search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un avertissement")
    public ResponseEntity<AvertissementDTO> trouverParId(@PathVariable UUID id) {
        return ResponseEntity.ok(avertissementService.trouverParId(id));
    }

    @PostMapping
    @Operation(summary = "Créer un avertissement")
    public ResponseEntity<AvertissementDTO> creer(@Valid @RequestBody AvertissementRequest request) {
        return ResponseEntity.ok(avertissementService.creer(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un avertissement")
    public ResponseEntity<AvertissementDTO> mettreAJour(
            @PathVariable UUID id,
            @Valid @RequestBody AvertissementRequest request) {
        return ResponseEntity.ok(avertissementService.mettreAJour(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un avertissement")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        avertissementService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/historique")
    @Operation(summary = "Historique des modifications d'un avertissement")
    public ResponseEntity<List<AuditDTO>> historique(@PathVariable UUID id) {
        return ResponseEntity.ok(auditService.historiqueEntite("avertissement", id.toString()));
    }

    @GetMapping("/{id}/derniere-modification")
    @Operation(summary = "Dernière modification d'un avertissement (anciennes valeurs)")
    public ResponseEntity<AuditDTO> derniereModification(@PathVariable UUID id) {
        AuditDTO audit = auditService.derniereModification("avertissement", id.toString());
        return audit != null ? ResponseEntity.ok(audit) : ResponseEntity.noContent().build();
    }

    // ── Recherches par critère ──

    @GetMapping("/personne/{idPersonne}")
    @Operation(summary = "Lister les avertissements par personne")
    public ResponseEntity<List<AvertissementDTO>> listerParPersonne(@PathVariable Integer idPersonne) {
        return ResponseEntity.ok(avertissementService.listerParPersonne(idPersonne));
    }

    @GetMapping("/annee/{annee}")
    @Operation(summary = "Lister les avertissements par année")
    public ResponseEntity<List<AvertissementDTO>> listerParAnnee(@PathVariable Integer annee) {
        return ResponseEntity.ok(avertissementService.listerParAnnee(annee));
    }

    @GetMapping("/titre-foncier/{idTitreFoncier}")
    @Operation(summary = "Lister les avertissements par titre foncier")
    public ResponseEntity<List<AvertissementDTO>> listerParTitreFoncier(
            @PathVariable UUID idTitreFoncier) {
        return ResponseEntity.ok(avertissementService.listerParTitreFoncier(idTitreFoncier));
    }

    @GetMapping("/parcelle/{idParcelle}")
    @Operation(summary = "Lister les avertissements par parcelle")
    public ResponseEntity<List<AvertissementDTO>> listerParParcelle(@PathVariable UUID idParcelle) {
        return ResponseEntity.ok(avertissementService.listerParParcelle(idParcelle));
    }

    // ── Statistiques ──

    @GetMapping("/stats")
    @Operation(summary = "Statistiques des avertissements")
    public ResponseEntity<Map<String, Object>> statistiques() {
        Map<String, Object> stats = Map.of(
                "total", avertissementService.lister(org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements()
        );
        return ResponseEntity.ok(stats);
    }
}
