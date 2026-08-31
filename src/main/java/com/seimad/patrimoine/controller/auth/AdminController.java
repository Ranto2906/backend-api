package com.seimad.patrimoine.controller.auth;

import com.seimad.patrimoine.dto.auth.JournalConnexionDTO;
import com.seimad.patrimoine.dto.auth.SessionDTO;
import com.seimad.patrimoine.service.auth.JournalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST pour le journal des connexions et les sessions.
 * Accès réservé aux Administrateurs.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Administration", description = "Journal des connexions, sessions, nettoyage")
public class AdminController {

    private final JournalService journalService;

    /**
     * GET /api/admin/journal — Journal paginé de toutes les connexions.
     */
    @GetMapping("/journal")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Journal des connexions",
               description = "Retourne le journal paginé de toutes les connexions (succès et échecs)")
    public ResponseEntity<Page<JournalConnexionDTO>> journal(Pageable pageable) {
        return ResponseEntity.ok(journalService.listerJournal(pageable));
    }

    /**
     * GET /api/admin/journal/{idUtilisateur} — Journal d'un utilisateur.
     */
    @GetMapping("/journal/{idUtilisateur}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Journal des connexions d'un utilisateur")
    public ResponseEntity<List<JournalConnexionDTO>> journalUtilisateur(
            @PathVariable Integer idUtilisateur) {
        return ResponseEntity.ok(journalService.listerParUtilisateur(idUtilisateur));
    }

    /**
     * GET /api/admin/sessions — Liste paginée de toutes les sessions.
     */
    @GetMapping("/sessions")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Lister les sessions",
               description = "Retourne toutes les sessions (actives et révoquées) de manière paginée")
    public ResponseEntity<Page<SessionDTO>> listerSessions(Pageable pageable) {
        return ResponseEntity.ok(journalService.listerSessions(pageable));
    }

    /**
     * GET /api/admin/sessions/{idUtilisateur} — Sessions d'un utilisateur.
     */
    @GetMapping("/sessions/{idUtilisateur}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Sessions d'un utilisateur")
    public ResponseEntity<List<SessionDTO>> sessionsUtilisateur(
            @PathVariable Integer idUtilisateur) {
        return ResponseEntity.ok(journalService.listerSessionsParUtilisateur(idUtilisateur));
    }

    /**
     * POST /api/admin/sessions/{idUtilisateur}/revoquer — Révoque toutes les sessions d'un utilisateur.
     */
    @PostMapping("/sessions/{idUtilisateur}/revoquer")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Révoquer toutes les sessions d'un utilisateur",
               description = "Force la déconnexion de l'utilisateur sur tous ses appareils")
    public ResponseEntity<Void> revoquerSessions(@PathVariable Integer idUtilisateur) {
        journalService.revoquerToutesLesSessions(idUtilisateur);
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /api/admin/sessions/expirees — Nettoie les sessions expirées.
     */
    @DeleteMapping("/sessions/expirees")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Nettoyer les sessions expirées")
    public ResponseEntity<Void> nettoyerSessionsExpirees() {
        journalService.nettoyerSessionsExpirees();
        return ResponseEntity.noContent().build();
    }
}
