package com.seimad.patrimoine.controller.notification;

import com.seimad.patrimoine.dto.notification.SuiviNotificationDTO;
import com.seimad.patrimoine.dto.notification.SuiviNotificationRequest;
import com.seimad.patrimoine.dto.dossier.AuditDTO;
import com.seimad.patrimoine.service.dossier.AuditService;
import com.seimad.patrimoine.service.notification.SuiviNotificationService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/suivis")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Suivi des Notifications", description = "Suivis rattachés aux notifications d'occupation")
public class SuiviNotificationController {

    private final SuiviNotificationService suiviService;
    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Lister les suivis (pagination)")
    public ResponseEntity<Page<SuiviNotificationDTO>> lister(Pageable pageable) {
        return ResponseEntity.ok(suiviService.lister(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des suivis")
    public ResponseEntity<Page<SuiviNotificationDTO>> rechercher(
            @RequestParam(defaultValue = "") String search,
            Pageable pageable) {
        return ResponseEntity.ok(suiviService.rechercher(search, pageable));
    }

    @GetMapping("/by-notification/{idNotification}")
    @Operation(summary = "Lister les suivis d'une notification")
    public ResponseEntity<List<SuiviNotificationDTO>> listerParNotification(
            @PathVariable UUID idNotification) {
        return ResponseEntity.ok(suiviService.listerParNotification(idNotification));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un suivi")
    public ResponseEntity<SuiviNotificationDTO> trouverParId(@PathVariable UUID id) {
        return ResponseEntity.ok(suiviService.trouverParId(id));
    }

    @PostMapping
    @Operation(summary = "Créer un suivi pour une notification")
    public ResponseEntity<SuiviNotificationDTO> creer(
            @Valid @RequestBody SuiviNotificationRequest request) {
        return ResponseEntity.ok(suiviService.creer(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un suivi")
    public ResponseEntity<SuiviNotificationDTO> mettreAJour(
            @PathVariable UUID id,
            @Valid @RequestBody SuiviNotificationRequest request) {
        return ResponseEntity.ok(suiviService.mettreAJour(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un suivi")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        suiviService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/historique")
    @Operation(summary = "Historique des modifications d'un suivi")
    public ResponseEntity<List<AuditDTO>> historique(@PathVariable UUID id) {
        return ResponseEntity.ok(auditService.historiqueEntite("suivi_notification", id.toString()));
    }

    @GetMapping("/{id}/derniere-modification")
    @Operation(summary = "Dernière modification d'un suivi (anciennes valeurs)")
    public ResponseEntity<AuditDTO> derniereModification(@PathVariable UUID id) {
        AuditDTO audit = auditService.derniereModification("suivi_notification", id.toString());
        return audit != null ? ResponseEntity.ok(audit) : ResponseEntity.noContent().build();
    }
}
