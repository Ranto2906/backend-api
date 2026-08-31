package com.seimad.patrimoine.controller.auth;

import com.seimad.patrimoine.dto.auth.*;
import com.seimad.patrimoine.service.auth.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Utilisateurs", description = "CRUD utilisateurs, activation/desactivation, reinitialisation mot de passe")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @GetMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Lister les utilisateurs")
    public ResponseEntity<Page<UtilisateurDTO>> lister(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<UtilisateurDTO> page = utilisateurService.lister(search, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Detail d'un utilisateur")
    public ResponseEntity<UtilisateurDTO> trouverParId(@PathVariable Integer id) {
        return ResponseEntity.ok(utilisateurService.trouverParId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Creer un utilisateur")
    public ResponseEntity<UtilisateurDTO> creer(
            @Valid @RequestBody CreateUtilisateurRequest request) {
        UtilisateurDTO dto = utilisateurService.creer(request);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Mettre a jour un utilisateur")
    public ResponseEntity<UtilisateurDTO> mettreAJour(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUtilisateurRequest request) {
        return ResponseEntity.ok(utilisateurService.mettreAJour(id, request));
    }

    @PatchMapping("/{id}/activer")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Activer/Desactiver un utilisateur")
    public ResponseEntity<UtilisateurDTO> activerDesactiver(
            @PathVariable Integer id,
            @RequestBody Map<String, Boolean> body) {
        boolean actif = body.getOrDefault("actif", true);
        return ResponseEntity.ok(utilisateurService.activerDesactiver(id, actif));
    }

    @PostMapping("/{id}/reinitialiser-mot-de-passe")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Reinitialiser le mot de passe")
    public ResponseEntity<Void> reinitialiserMotDePasse(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        String nouveauMotDePasse = body.get("motDePasse");
        if (nouveauMotDePasse == null || nouveauMotDePasse.length() < 6) {
            return ResponseEntity.badRequest().build();
        }
        utilisateurService.reinitialiserMotDePasse(id, nouveauMotDePasse);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Supprimer un utilisateur")
    public ResponseEntity<Void> supprimer(@PathVariable Integer id) {
        utilisateurService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Lister les roles d'un utilisateur")
    public ResponseEntity<List<RoleDTO>> listerRoles(@PathVariable Integer id) {
        return ResponseEntity.ok(utilisateurService.listerRoles(id));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Attribuer un role a un utilisateur")
    public ResponseEntity<Void> attribuerRole(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> body) {
        Integer idRole = body.get("idRole");
        if (idRole == null) {
            return ResponseEntity.badRequest().build();
        }
        utilisateurService.attribuerRole(id, idRole);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Retirer un role a un utilisateur")
    public ResponseEntity<Void> retirerRole(
            @PathVariable Integer id,
            @PathVariable Integer roleId) {
        utilisateurService.retirerRole(id, roleId);
        return ResponseEntity.noContent().build();
    }
}
