package com.seimad.patrimoine.controller.referentiel;

import com.seimad.patrimoine.dto.referentiel.PrixM2DTO;
import com.seimad.patrimoine.dto.referentiel.PrixM2Request;
import com.seimad.patrimoine.service.referentiel.PrixM2Service;
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
@RequestMapping("/api/prix-m2")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Prix au m²", description = "CRUD prix au mètre carré par ville")
public class PrixM2Controller {

    private final PrixM2Service prixM2Service;

    @GetMapping
    @Operation(summary = "Lister les prix (pagination)")
    public ResponseEntity<Page<PrixM2DTO>> lister(Pageable pageable) {
        return ResponseEntity.ok(prixM2Service.lister(pageable));
    }

    @GetMapping("/ville/{idVille}")
    @Operation(summary = "Lister les prix d'une ville")
    public ResponseEntity<List<PrixM2DTO>> listerParVille(@PathVariable Integer idVille) {
        return ResponseEntity.ok(prixM2Service.listerParVille(idVille));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un prix")
    public ResponseEntity<PrixM2DTO> trouverParId(@PathVariable Integer id) {
        return ResponseEntity.ok(prixM2Service.trouverParId(id));
    }

    @PostMapping
    @Operation(summary = "Créer un prix")
    public ResponseEntity<PrixM2DTO> creer(@Valid @RequestBody PrixM2Request request) {
        return ResponseEntity.ok(prixM2Service.creer(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un prix")
    public ResponseEntity<PrixM2DTO> mettreAJour(@PathVariable Integer id, @Valid @RequestBody PrixM2Request request) {
        return ResponseEntity.ok(prixM2Service.mettreAJour(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un prix")
    public ResponseEntity<Void> supprimer(@PathVariable Integer id) {
        prixM2Service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
