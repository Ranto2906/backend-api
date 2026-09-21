package com.seimad.patrimoine.controller.dossier;

import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.dossier.DossierFournie;
import com.seimad.patrimoine.repository.auth.UtilisateurRepository;
import com.seimad.patrimoine.repository.dossier.DossierFournieRepository;
import com.seimad.patrimoine.service.dossier.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dossiers-fournis")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Dossiers Fournis", description = "CRUD du référentiel des pièces/dossiers fournis")
public class DossierFournieController {

    private final DossierFournieRepository dossierFournieRepository;
    private final AuditService auditService;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    @Operation(summary = "Lister les dossiers fournis")
    public ResponseEntity<List<DossierFournie>> lister() {
        return ResponseEntity.ok(dossierFournieRepository.findAll());
    }

    @PostMapping
    @Operation(summary = "Créer un dossier fourni")
    public ResponseEntity<DossierFournie> creer(@Valid @RequestBody DossierFournie request) {
        String libelle = request.getLibelle() == null ? "" : request.getLibelle().trim();
        if (libelle.isEmpty()) {
            throw new RuntimeException("Le libellé du dossier fourni est obligatoire");
        }
        dossierFournieRepository.findByLibelleIgnoreCase(libelle).ifPresent(d -> {
            throw new RuntimeException("Le dossier fourni « " + libelle + " » existe déjà");
        });
        DossierFournie saved = dossierFournieRepository.save(DossierFournie.builder().libelle(libelle).build());
        auditService.enregistrer("dossier_fournie", saved.getIdDossierFournie().toString(), "CREATE",
                null, Map.<String, Object>of("libelle", saved.getLibelle()), utilisateurConnecte());
        log.info("Dossier fourni créé : {}", saved.getLibelle());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un dossier fourni")
    public ResponseEntity<DossierFournie> mettreAJour(@PathVariable Integer id, @Valid @RequestBody DossierFournie request) {
        DossierFournie dossierFournie = dossierFournieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier fourni non trouvé avec l'id : " + id));
        String libelle = request.getLibelle() == null ? "" : request.getLibelle().trim();
        if (libelle.isEmpty()) {
            throw new RuntimeException("Le libellé du dossier fourni est obligatoire");
        }
        dossierFournieRepository.findByLibelleIgnoreCase(libelle)
                .filter(d -> !d.getIdDossierFournie().equals(id))
                .ifPresent(d -> {
                    throw new RuntimeException("Le dossier fourni « " + libelle + " » existe déjà");
                });
        Map<String, Object> anciennes = Map.<String, Object>of("libelle", dossierFournie.getLibelle());
        dossierFournie.setLibelle(libelle);
        DossierFournie saved = dossierFournieRepository.save(dossierFournie);
        auditService.enregistrer("dossier_fournie", id.toString(), "UPDATE",
                anciennes, Map.<String, Object>of("libelle", saved.getLibelle()), utilisateurConnecte());
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un dossier fourni")
    public ResponseEntity<Void> supprimer(@PathVariable Integer id) {
        DossierFournie dossierFournie = dossierFournieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier fourni non trouvé avec l'id : " + id));
        dossierFournieRepository.delete(dossierFournie);
        auditService.enregistrer("dossier_fournie", id.toString(), "DELETE",
                Map.<String, Object>of("libelle", dossierFournie.getLibelle()), null, utilisateurConnecte());
        log.info("Dossier fourni supprimé : {}", dossierFournie.getLibelle());
        return ResponseEntity.noContent().build();
    }

    private Utilisateur utilisateurConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getPrincipal())) return null;
        return utilisateurRepository.findByNomUtilisateur(auth.getName()).orElse(null);
    }
}
