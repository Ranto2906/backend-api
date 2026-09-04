package com.seimad.patrimoine.controller.notification;

import com.seimad.patrimoine.dto.dossier.AuditDTO;
import com.seimad.patrimoine.dto.notification.NotificationOccupationDTO;
import com.seimad.patrimoine.dto.notification.NotificationOccupationRequest;
import com.seimad.patrimoine.service.dossier.AuditService;
import com.seimad.patrimoine.service.notification.NotificationService;
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
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Notifications d'Occupation", description = "CRUD notifications d'occupation et suivi")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Lister les notifications (pagination)")
    public ResponseEntity<Page<NotificationOccupationDTO>> lister(Pageable pageable) {
        return ResponseEntity.ok(notificationService.lister(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des notifications")
    public ResponseEntity<Page<NotificationOccupationDTO>> rechercher(
            @RequestParam(defaultValue = "") String search,
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.rechercher(search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une notification")
    public ResponseEntity<NotificationOccupationDTO> trouverParId(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.trouverParId(id));
    }

    @PostMapping
    @Operation(summary = "Créer une notification")
    public ResponseEntity<NotificationOccupationDTO> creer(
            @Valid @RequestBody NotificationOccupationRequest request) {
        return ResponseEntity.ok(notificationService.creer(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une notification")
    public ResponseEntity<NotificationOccupationDTO> mettreAJour(
            @PathVariable UUID id,
            @Valid @RequestBody NotificationOccupationRequest request) {
        return ResponseEntity.ok(notificationService.mettreAJour(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une notification")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        notificationService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/historique")
    @Operation(summary = "Historique des modifications d'une notification")
    public ResponseEntity<List<AuditDTO>> historique(@PathVariable UUID id) {
        return ResponseEntity.ok(auditService.historiqueEntite("notification_occupation", id.toString()));
    }

    @GetMapping("/{id}/derniere-modification")
    @Operation(summary = "Dernière modification d'une notification (anciennes valeurs)")
    public ResponseEntity<AuditDTO> derniereModification(@PathVariable UUID id) {
        AuditDTO audit = auditService.derniereModification("notification_occupation", id.toString());
        return audit != null ? ResponseEntity.ok(audit) : ResponseEntity.noContent().build();
    }

    // ── Recherches par critère ──

    @GetMapping("/statut/{statut}")
    @Operation(summary = "Lister les notifications par statut")
    public ResponseEntity<List<NotificationOccupationDTO>> listerParStatut(@PathVariable String statut) {
        return ResponseEntity.ok(notificationService.listerParStatut(statut));
    }

    @GetMapping("/annee/{annee}")
    @Operation(summary = "Lister les notifications par année")
    public ResponseEntity<List<NotificationOccupationDTO>> listerParAnnee(@PathVariable Integer annee) {
        return ResponseEntity.ok(notificationService.listerParAnnee(annee));
    }

    @GetMapping("/titre-foncier/{idTitreFoncier}")
    @Operation(summary = "Lister les notifications par titre foncier")
    public ResponseEntity<List<NotificationOccupationDTO>> listerParTitreFoncier(
            @PathVariable UUID idTitreFoncier) {
        return ResponseEntity.ok(notificationService.listerParTitreFoncier(idTitreFoncier));
    }

    @GetMapping("/parcelle/{idParcelle}")
    @Operation(summary = "Lister les notifications par parcelle")
    public ResponseEntity<List<NotificationOccupationDTO>> listerParParcelle(
            @PathVariable UUID idParcelle) {
        return ResponseEntity.ok(notificationService.listerParParcelle(idParcelle));
    }

    // ── Statistiques ──

    @GetMapping("/stats")
    @Operation(summary = "Statistiques des notifications")
    public ResponseEntity<Map<String, Object>> statistiques() {
        Map<String, Object> stats = Map.of(
                "enCours", notificationService.compterParStatut("En cours"),
                "traitees", notificationService.compterParStatut("Traité"),
                "cloturees", notificationService.compterParStatut("Clôturée")
        );
        return ResponseEntity.ok(stats);
    }
}
