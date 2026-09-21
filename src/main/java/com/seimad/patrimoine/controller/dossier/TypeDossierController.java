package com.seimad.patrimoine.controller.dossier;

import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.dossier.TypeDossier;
import com.seimad.patrimoine.repository.auth.UtilisateurRepository;
import com.seimad.patrimoine.repository.dossier.TypeDossierRepository;
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
@RequestMapping("/api/types-dossier")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Types de Dossier", description = "CRUD du référentiel des types de dossier")
public class TypeDossierController {

    private final TypeDossierRepository typeDossierRepository;
    private final AuditService auditService;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    @Operation(summary = "Lister les types de dossier")
    public ResponseEntity<List<TypeDossier>> lister() {
        return ResponseEntity.ok(typeDossierRepository.findAll());
    }

    @PostMapping
    @Operation(summary = "Créer un type de dossier")
    public ResponseEntity<TypeDossier> creer(@Valid @RequestBody TypeDossier request) {
        String code = request.getCode() == null ? "" : request.getCode().trim().toUpperCase();
        String libelle = request.getLibelle() == null ? "" : request.getLibelle().trim();
        if (code.isEmpty()) {
            throw new RuntimeException("Le code du type de dossier est obligatoire");
        }
        if (libelle.isEmpty()) {
            throw new RuntimeException("Le libellé du type de dossier est obligatoire");
        }
        typeDossierRepository.findByCodeIgnoreCase(code).ifPresent(t -> {
            throw new RuntimeException("Le code « " + code + " » existe déjà pour un type de dossier");
        });
        typeDossierRepository.findByLibelleIgnoreCase(libelle).ifPresent(t -> {
            throw new RuntimeException("Le type de dossier « " + libelle + " » existe déjà");
        });
        TypeDossier saved = typeDossierRepository.save(TypeDossier.builder()
                .code(code)
                .libelle(libelle)
                .build());
        auditService.enregistrer("type_dossier", saved.getIdTypeDossier().toString(), "CREATE",
                null, Map.<String, Object>of("code", saved.getCode(), "libelle", saved.getLibelle()), utilisateurConnecte());
        log.info("Type de dossier créé : {} ({})", saved.getCode(), saved.getLibelle());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un type de dossier")
    public ResponseEntity<TypeDossier> mettreAJour(@PathVariable Integer id, @Valid @RequestBody TypeDossier request) {
        TypeDossier type = typeDossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Type de dossier non trouvé avec l'id : " + id));
        String code = request.getCode() == null ? "" : request.getCode().trim().toUpperCase();
        String libelle = request.getLibelle() == null ? "" : request.getLibelle().trim();
        if (code.isEmpty()) {
            throw new RuntimeException("Le code du type de dossier est obligatoire");
        }
        if (libelle.isEmpty()) {
            throw new RuntimeException("Le libellé du type de dossier est obligatoire");
        }
        typeDossierRepository.findByCodeIgnoreCase(code)
                .filter(t -> !t.getIdTypeDossier().equals(id))
                .ifPresent(t -> {
                    throw new RuntimeException("Le code « " + code + " » existe déjà pour un type de dossier");
                });
        typeDossierRepository.findByLibelleIgnoreCase(libelle)
                .filter(t -> !t.getIdTypeDossier().equals(id))
                .ifPresent(t -> {
                    throw new RuntimeException("Le type de dossier « " + libelle + " » existe déjà");
                });
        Map<String, Object> anciennes = Map.<String, Object>of("code", type.getCode(), "libelle", type.getLibelle());
        type.setCode(code);
        type.setLibelle(libelle);
        TypeDossier saved = typeDossierRepository.save(type);
        auditService.enregistrer("type_dossier", id.toString(), "UPDATE",
                anciennes, Map.<String, Object>of("code", saved.getCode(), "libelle", saved.getLibelle()), utilisateurConnecte());
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un type de dossier")
    public ResponseEntity<Void> supprimer(@PathVariable Integer id) {
        TypeDossier type = typeDossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Type de dossier non trouvé avec l'id : " + id));
        typeDossierRepository.delete(type);
        auditService.enregistrer("type_dossier", id.toString(), "DELETE",
                Map.<String, Object>of("code", type.getCode(), "libelle", type.getLibelle()), null, utilisateurConnecte());
        log.info("Type de dossier supprimé : {}", type.getLibelle());
        return ResponseEntity.noContent().build();
    }

    private Utilisateur utilisateurConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getPrincipal())) return null;
        return utilisateurRepository.findByNomUtilisateur(auth.getName()).orElse(null);
    }
}
