package com.seimad.patrimoine.controller.referentiel;

import com.seimad.patrimoine.dto.referentiel.ProprieteDTO;
import com.seimad.patrimoine.dto.referentiel.ProprieteRequest;
import com.seimad.patrimoine.service.referentiel.ProprieteService;
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
@RequestMapping("/api/proprietes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Propriétés", description = "CRUD propriétés du patrimoine foncier")
public class ProprieteController {

    private final ProprieteService proprieteService;

    @GetMapping
    @Operation(summary = "Lister les propriétés (pagination)")
    public ResponseEntity<Page<ProprieteDTO>> lister(Pageable pageable) {
        return ResponseEntity.ok(proprieteService.lister(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des propriétés")
    public ResponseEntity<List<ProprieteDTO>> rechercher(@RequestParam String search) {
        return ResponseEntity.ok(proprieteService.rechercher(search));
    }

    @GetMapping("/ville/{idVille}")
    @Operation(summary = "Lister les propriétés d'une ville")
    public ResponseEntity<List<ProprieteDTO>> listerParVille(@PathVariable Integer idVille) {
        return ResponseEntity.ok(proprieteService.listerParVille(idVille));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une propriété")
    public ResponseEntity<ProprieteDTO> trouverParId(@PathVariable Integer id) {
        return ResponseEntity.ok(proprieteService.trouverParId(id));
    }

    @PostMapping
    @Operation(summary = "Créer une propriété")
    public ResponseEntity<ProprieteDTO> creer(@Valid @RequestBody ProprieteRequest request) {
        return ResponseEntity.ok(proprieteService.creer(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une propriété")
    public ResponseEntity<ProprieteDTO> mettreAJour(@PathVariable Integer id, @Valid @RequestBody ProprieteRequest request) {
        return ResponseEntity.ok(proprieteService.mettreAJour(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une propriété")
    public ResponseEntity<Void> supprimer(@PathVariable Integer id) {
        proprieteService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
