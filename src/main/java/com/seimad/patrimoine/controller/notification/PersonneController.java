package com.seimad.patrimoine.controller.notification;

import com.seimad.patrimoine.dto.notification.PersonneDTO;
import com.seimad.patrimoine.dto.notification.PersonneRequest;
import com.seimad.patrimoine.service.notification.PersonneService;
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
@RequestMapping("/api/personnes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Personnes", description = "CRUD personnes pour les notifications et avertissements")
public class PersonneController {

    private final PersonneService personneService;

    @GetMapping
    @Operation(summary = "Lister les personnes (pagination)")
    public ResponseEntity<Page<PersonneDTO>> lister(Pageable pageable) {
        return ResponseEntity.ok(personneService.lister(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des personnes")
    public ResponseEntity<Page<PersonneDTO>> rechercher(
            @RequestParam(defaultValue = "") String search,
            Pageable pageable) {
        return ResponseEntity.ok(personneService.rechercher(search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une personne")
    public ResponseEntity<PersonneDTO> trouverParId(@PathVariable Integer id) {
        return ResponseEntity.ok(personneService.trouverParId(id));
    }

    @PostMapping
    @Operation(summary = "Créer une personne")
    public ResponseEntity<PersonneDTO> creer(@Valid @RequestBody PersonneRequest request) {
        return ResponseEntity.ok(personneService.creer(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une personne")
    public ResponseEntity<PersonneDTO> mettreAJour(
            @PathVariable Integer id,
            @Valid @RequestBody PersonneRequest request) {
        return ResponseEntity.ok(personneService.mettreAJour(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une personne")
    public ResponseEntity<Void> supprimer(@PathVariable Integer id) {
        personneService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Lister les personnes par rôle")
    public ResponseEntity<List<PersonneDTO>> listerParRole(@PathVariable String role) {
        return ResponseEntity.ok(personneService.listerParRole(role));
    }

    @GetMapping("/search/nom")
    @Operation(summary = "Rechercher des personnes par nom")
    public ResponseEntity<List<PersonneDTO>> rechercherParNom(@RequestParam String nom) {
        return ResponseEntity.ok(personneService.rechercherParNom(nom));
    }
}
