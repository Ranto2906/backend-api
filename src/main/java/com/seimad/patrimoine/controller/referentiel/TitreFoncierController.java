package com.seimad.patrimoine.controller.referentiel;

import com.seimad.patrimoine.dto.referentiel.TitreFoncierDTO;
import com.seimad.patrimoine.dto.referentiel.TitreFoncierRequest;
import com.seimad.patrimoine.service.referentiel.TitreFoncierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/titres-fonciers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Titres Fonciers", description = "CRUD titres fonciers et parcelles cadastrales")
public class TitreFoncierController {

    private final TitreFoncierService titreFoncierService;

    @GetMapping
    @Operation(summary = "Lister les titres fonciers (pagination)")
    public ResponseEntity<Page<TitreFoncierDTO>> lister(Pageable pageable) {
        return ResponseEntity.ok(titreFoncierService.lister(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un titre foncier")
    public ResponseEntity<TitreFoncierDTO> trouverParId(@PathVariable UUID id) {
        return ResponseEntity.ok(titreFoncierService.trouverParId(id));
    }

    @GetMapping("/numero/{numero}")
    @Operation(summary = "Trouver un titre foncier par numéro")
    public ResponseEntity<TitreFoncierDTO> trouverParNumero(@PathVariable String numero) {
        return ResponseEntity.ok(titreFoncierService.trouverParNumero(numero));
    }

    @PostMapping
    @Operation(summary = "Créer un titre foncier")
    public ResponseEntity<TitreFoncierDTO> creer(@Valid @RequestBody TitreFoncierRequest request) {
        return ResponseEntity.ok(titreFoncierService.creer(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un titre foncier")
    public ResponseEntity<TitreFoncierDTO> mettreAJour(@PathVariable UUID id, @Valid @RequestBody TitreFoncierRequest request) {
        return ResponseEntity.ok(titreFoncierService.mettreAJour(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un titre foncier")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        titreFoncierService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
