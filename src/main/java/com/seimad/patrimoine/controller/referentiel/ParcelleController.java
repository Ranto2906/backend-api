package com.seimad.patrimoine.controller.referentiel;

import com.seimad.patrimoine.dto.referentiel.ParcelleDTO;
import com.seimad.patrimoine.dto.referentiel.ParcelleRequest;
import com.seimad.patrimoine.service.referentiel.ParcelleService;
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
@RequestMapping("/api/parcelles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Parcelles", description = "CRUD parcelles rattachées à un titre foncier")
public class ParcelleController {

    private final ParcelleService parcelleService;

    @GetMapping
    @Operation(summary = "Lister les parcelles (pagination)")
    public ResponseEntity<Page<ParcelleDTO>> lister(Pageable pageable) {
        return ResponseEntity.ok(parcelleService.lister(pageable));
    }

    @GetMapping("/titre-foncier/{idTitreFoncier}")
    @Operation(summary = "Lister les parcelles d'un titre foncier")
    public ResponseEntity<List<ParcelleDTO>> listerParTitreFoncier(@PathVariable UUID idTitreFoncier) {
        return ResponseEntity.ok(parcelleService.listerParTitreFoncier(idTitreFoncier));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une parcelle")
    public ResponseEntity<ParcelleDTO> trouverParId(@PathVariable UUID id) {
        return ResponseEntity.ok(parcelleService.trouverParId(id));
    }

    @PostMapping
    @Operation(summary = "Créer une parcelle")
    public ResponseEntity<ParcelleDTO> creer(@Valid @RequestBody ParcelleRequest request) {
        return ResponseEntity.ok(parcelleService.creer(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une parcelle")
    public ResponseEntity<ParcelleDTO> mettreAJour(@PathVariable UUID id, @Valid @RequestBody ParcelleRequest request) {
        return ResponseEntity.ok(parcelleService.mettreAJour(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une parcelle")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        parcelleService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
