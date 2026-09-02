package com.seimad.patrimoine.controller.notification;

import com.seimad.patrimoine.dto.notification.SuiviNotificationDTO;
import com.seimad.patrimoine.dto.notification.SuiviNotificationRequest;
import com.seimad.patrimoine.service.notification.SuiviNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/{idNotification}/suivis")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Suivi des Notifications", description = "Gestion du suivi des notifications d'occupation")
public class SuiviNotificationController {

    private final SuiviNotificationService suiviService;

    @GetMapping
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
            @PathVariable UUID idNotification,
            @Valid @RequestBody SuiviNotificationRequest request) {
        return ResponseEntity.ok(suiviService.creer(idNotification, request));
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
}
