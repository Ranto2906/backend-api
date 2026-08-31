package com.seimad.patrimoine.controller.auth;

import com.seimad.patrimoine.dto.auth.*;
import com.seimad.patrimoine.service.auth.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Roles et Permissions", description = "CRUD roles, permissions RBAC, modules, entites, actions")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Lister les roles")
    public ResponseEntity<List<RoleDTO>> lister() {
        return ResponseEntity.ok(roleService.lister());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Detail d'un role")
    public ResponseEntity<RoleDTO> trouverParId(@PathVariable Integer id) {
        return ResponseEntity.ok(roleService.trouverParId(id));
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Role avec ses permissions")
    public ResponseEntity<RoleWithPermissionsDTO> trouverAvecPermissions(
            @PathVariable Integer id) {
        return ResponseEntity.ok(roleService.trouverAvecPermissions(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Creer un role")
    public ResponseEntity<RoleDTO> creer(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(roleService.creer(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Mettre a jour un role")
    public ResponseEntity<RoleDTO> mettreAJour(
            @PathVariable Integer id,
            @Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(roleService.mettreAJour(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Supprimer un role")
    public ResponseEntity<Void> supprimer(@PathVariable Integer id) {
        roleService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Lister toutes les permissions")
    public ResponseEntity<List<PermissionDTO>> listerPermissions() {
        return ResponseEntity.ok(roleService.listerPermissions());
    }

    @PostMapping("/{idRole}/permissions")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Attribuer une permission a un role")
    public ResponseEntity<Void> attribuerPermission(
            @PathVariable Integer idRole,
            @RequestBody Map<String, Integer> body) {
        Integer idPermission = body.get("idPermission");
        if (idPermission == null) return ResponseEntity.badRequest().build();
        roleService.attribuerPermission(idRole, idPermission);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{idRole}/permissions/{idPermission}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Retirer une permission d'un role")
    public ResponseEntity<Void> retirerPermission(
            @PathVariable Integer idRole,
            @PathVariable Integer idPermission) {
        roleService.retirerPermission(idRole, idPermission);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/modules")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Lister les modules RBAC")
    public ResponseEntity<List<Map<String, Object>>> listerModules() {
        return ResponseEntity.ok(roleService.listerModules());
    }

    @GetMapping("/entites")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Lister les entites RBAC")
    public ResponseEntity<List<Map<String, Object>>> listerEntites() {
        return ResponseEntity.ok(roleService.listerEntites());
    }

    @GetMapping("/actions")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Lister les actions RBAC")
    public ResponseEntity<List<Map<String, Object>>> listerActions() {
        return ResponseEntity.ok(roleService.listerActions());
    }
}
