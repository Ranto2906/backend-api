package com.seimad.patrimoine.controller.referentiel;

import com.seimad.patrimoine.dto.referentiel.VilleDTO;
import com.seimad.patrimoine.dto.referentiel.VilleRequest;
import com.seimad.patrimoine.service.referentiel.VilleService;
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

@RestController
@RequestMapping("/api/villes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Villes", description = "CRUD villes du patrimoine foncier")
public class VilleController {

    private final VilleService villeService;

    @GetMapping
    @Operation(summary = "Lister les villes (pagination)")
    public ResponseEntity<Page<VilleDTO>> lister(Pageable pageable) {
        return ResponseEntity.ok(villeService.lister(pageable));
    }

    @GetMapping("/all")
    @Operation(summary = "Lister toutes les villes (sans pagination)")
    public ResponseEntity<List<VilleDTO>> listerToutes() {
        return ResponseEntity.ok(villeService.listerToutes());
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des villes")
    public ResponseEntity<List<VilleDTO>> rechercher(@RequestParam String search) {
        return ResponseEntity.ok(villeService.rechercher(search));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une ville")
    public ResponseEntity<VilleDTO> trouverParId(@PathVariable Integer id) {
        return ResponseEntity.ok(villeService.trouverParId(id));
    }

    @PostMapping
    @Operation(summary = "Créer une ville")
    public ResponseEntity<VilleDTO> creer(@Valid @RequestBody VilleRequest request) {
        return ResponseEntity.ok(villeService.creer(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une ville")
    public ResponseEntity<VilleDTO> mettreAJour(@PathVariable Integer id, @Valid @RequestBody VilleRequest request) {
        return ResponseEntity.ok(villeService.mettreAJour(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une ville")
    public ResponseEntity<Void> supprimer(@PathVariable Integer id) {
        villeService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
