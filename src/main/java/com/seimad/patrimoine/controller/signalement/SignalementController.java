package com.seimad.patrimoine.controller.signalement;

import com.seimad.patrimoine.dto.dossier.AuditDTO;
import com.seimad.patrimoine.dto.signalement.SignalementDTO;
import com.seimad.patrimoine.dto.signalement.SignalementRequest;
import com.seimad.patrimoine.dto.signalement.SignalementTraitementRequest;
import com.seimad.patrimoine.dto.signalement.StatutSignalementDTO;
import com.seimad.patrimoine.dto.signalement.TypeSignalementDTO;
import com.seimad.patrimoine.service.dossier.AuditService;
import com.seimad.patrimoine.service.signalement.SignalementService;
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
@RequestMapping("/api/signalements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Signalements", description = "CRUD signalements, traitement, historique")
public class SignalementController {

    private final SignalementService signalementService;
    private final AuditService auditService;

    // ── Référentiels ──

    @GetMapping("/types")
    @Operation(summary = "Lister les types de signalement")
    public ResponseEntity<List<TypeSignalementDTO>> listerTypes() {
        return ResponseEntity.ok(signalementService.listerTypes());
    }

    @GetMapping("/statuts")
    @Operation(summary = "Lister les statuts de signalement")
    public ResponseEntity<List<StatutSignalementDTO>> listerStatuts() {
        return ResponseEntity.ok(signalementService.listerStatuts());
    }

    // ── CRUD ──

    @GetMapping
    @Operation(summary = "Lister les signalements (pagination)")
    public ResponseEntity<Page<SignalementDTO>> lister(Pageable pageable) {
        return ResponseEntity.ok(signalementService.lister(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des signalements")
    public ResponseEntity<Page<SignalementDTO>> rechercher(
            @RequestParam(defaultValue = "") String search,
            Pageable pageable) {
        return ResponseEntity.ok(signalementService.rechercher(search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un signalement")
    public ResponseEntity<SignalementDTO> trouverParId(@PathVariable UUID id) {
        return ResponseEntity.ok(signalementService.trouverParId(id));
    }

    @PostMapping
    @Operation(summary = "Créer un signalement (référence auto SIG-AAAA-NNNN si absente)")
    public ResponseEntity<SignalementDTO> creer(@Valid @RequestBody SignalementRequest request) {
        return ResponseEntity.ok(signalementService.creer(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un signalement")
    public ResponseEntity<SignalementDTO> mettreAJour(
            @PathVariable UUID id,
            @Valid @RequestBody SignalementRequest request) {
        return ResponseEntity.ok(signalementService.mettreAJour(id, request));
    }

    @PostMapping("/{id}/traitement")
    @Operation(summary = "Traiter un signalement (statut + commentaire + date de traitement)")
    public ResponseEntity<SignalementDTO> traiter(
            @PathVariable UUID id,
            @Valid @RequestBody SignalementTraitementRequest request) {
        return ResponseEntity.ok(signalementService.traiter(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un signalement")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        signalementService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    // ── Historique ──

    @GetMapping("/{id}/historique")
    @Operation(summary = "Historique des modifications d'un signalement")
    public ResponseEntity<List<AuditDTO>> historique(@PathVariable UUID id) {
        return ResponseEntity.ok(auditService.historiqueEntite("signalement", id.toString()));
    }

    @GetMapping("/{id}/derniere-modification")
    @Operation(summary = "Dernière modification d'un signalement")
    public ResponseEntity<AuditDTO> derniereModification(@PathVariable UUID id) {
        AuditDTO audit = auditService.derniereModification("signalement", id.toString());
        return audit != null ? ResponseEntity.ok(audit) : ResponseEntity.noContent().build();
    }

    // ── Filtres ──

    @GetMapping("/ville/{idVille}")
    @Operation(summary = "Lister les signalements par ville")
    public ResponseEntity<List<SignalementDTO>> listerParVille(@PathVariable Integer idVille) {
        return ResponseEntity.ok(signalementService.listerParVille(idVille));
    }

    @GetMapping("/statut/{idStatut}")
    @Operation(summary = "Lister les signalements par statut")
    public ResponseEntity<List<SignalementDTO>> listerParStatut(@PathVariable Integer idStatut) {
        return ResponseEntity.ok(signalementService.listerParStatut(idStatut));
    }

    @GetMapping("/type/{idType}")
    @Operation(summary = "Lister les signalements par type")
    public ResponseEntity<List<SignalementDTO>> listerParType(@PathVariable Integer idType) {
        return ResponseEntity.ok(signalementService.listerParType(idType));
    }

    @GetMapping("/titre-foncier/{idTitreFoncier}")
    @Operation(summary = "Lister les signalements par titre foncier")
    public ResponseEntity<List<SignalementDTO>> listerParTitreFoncier(@PathVariable UUID idTitreFoncier) {
        return ResponseEntity.ok(signalementService.listerParTitreFoncier(idTitreFoncier));
    }

    @GetMapping("/parcelle/{idParcelle}")
    @Operation(summary = "Lister les signalements par parcelle")
    public ResponseEntity<List<SignalementDTO>> listerParParcelle(@PathVariable UUID idParcelle) {
        return ResponseEntity.ok(signalementService.listerParParcelle(idParcelle));
    }

    // ── Statistiques ──

    @GetMapping("/stats")
    @Operation(summary = "Statistiques des signalements (total, par statut, par type)")
    public ResponseEntity<Map<String, Object>> statistiques() {
        return ResponseEntity.ok(signalementService.statistiques());
    }
}