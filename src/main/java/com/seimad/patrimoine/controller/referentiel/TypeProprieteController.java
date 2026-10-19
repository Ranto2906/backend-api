package com.seimad.patrimoine.controller.referentiel;

import com.seimad.patrimoine.dto.referentiel.TypeProprieteDTO;
import com.seimad.patrimoine.dto.referentiel.TypeProprieteRequest;
import com.seimad.patrimoine.service.referentiel.TypeProprieteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/types-propriete")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des types de propriété",
     description = "Référentiel des types de propriété (Indéterminé, Parcelle, etc.)")
public class TypeProprieteController {

    private final TypeProprieteService typeProprieteService;

    @GetMapping
    @Operation(summary = "Lister les types de propriété")
    public ResponseEntity<List<TypeProprieteDTO>> lister() {
        return ResponseEntity.ok(typeProprieteService.lister());
    }

    @GetMapping("/par-defaut")
    @Operation(summary = "Type de propriété par défaut (Indéterminé)")
    public ResponseEntity<TypeProprieteDTO> trouverParDefaut() {
        return ResponseEntity.ok(typeProprieteService.trouverParDefaut());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un type de propriété")
    public ResponseEntity<TypeProprieteDTO> trouverParId(@PathVariable Integer id) {
        return ResponseEntity.ok(typeProprieteService.trouverParId(id));
    }

    @PostMapping
    @Operation(summary = "Créer un type de propriété")
    public ResponseEntity<TypeProprieteDTO> creer(@Valid @RequestBody TypeProprieteRequest request) {
        return ResponseEntity.ok(typeProprieteService.creer(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un type de propriété")
    public ResponseEntity<TypeProprieteDTO> mettreAJour(
            @PathVariable Integer id,
            @Valid @RequestBody TypeProprieteRequest request) {
        return ResponseEntity.ok(typeProprieteService.mettreAJour(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un type de propriété (refusé s'il est utilisé)")
    public ResponseEntity<Void> supprimer(@PathVariable Integer id) {
        typeProprieteService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
