package com.seimad.patrimoine.controller.referentiel;

import com.seimad.patrimoine.dto.referentiel.LieuDTO;
import com.seimad.patrimoine.dto.referentiel.LieuRequest;
import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.referentiel.Lieu;
import com.seimad.patrimoine.entity.referentiel.Ville;
import com.seimad.patrimoine.repository.auth.UtilisateurRepository;
import com.seimad.patrimoine.repository.referentiel.LieuRepository;
import com.seimad.patrimoine.repository.referentiel.VilleRepository;
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
@RequestMapping("/api/lieux")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Lieux", description = "CRUD du référentiel des lieux rattachés aux villes")
public class LieuController {

    private final LieuRepository lieuRepository;
    private final VilleRepository villeRepository;
    private final AuditService auditService;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    @Operation(summary = "Lister les lieux")
    public ResponseEntity<List<LieuDTO>> lister(@RequestParam(required = false) Integer idVille) {
        List<Lieu> lieux;
        if (idVille == null) {
            lieux = lieuRepository.findAll();
        } else {
            lieux = lieuRepository.findByVilleIdVilleOrderByLibelleAsc(idVille);
        }
        return ResponseEntity.ok(lieux.stream().map(this::toDTO).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un lieu")
    public ResponseEntity<LieuDTO> trouverParId(@PathVariable Integer id) {
        return lieuRepository.findById(id)
                .map(lieu -> ResponseEntity.ok(toDTO(lieu)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Créer un lieu")
    public ResponseEntity<LieuDTO> creer(@Valid @RequestBody LieuRequest request) {
        String libelle = request.getLibelle() == null ? "" : request.getLibelle().trim();
        if (libelle.isEmpty()) {
            throw new RuntimeException("Le libellé du lieu est obligatoire");
        }
        Ville ville = villeRepository.findById(request.getIdVille())
                .orElseThrow(() -> new RuntimeException("Ville non trouvée avec l'id : " + request.getIdVille()));
        lieuRepository.findByVilleIdVilleAndLibelleIgnoreCase(request.getIdVille(), libelle).ifPresent(l -> {
            throw new RuntimeException("Le lieu « " + libelle + " » existe déjà dans la ville " + ville.getNomVille());
        });
        Lieu saved = lieuRepository.save(Lieu.builder().libelle(libelle).ville(ville).build());
        auditService.enregistrer("lieu", saved.getIdLieu().toString(), "CREATE",
                null, Map.<String, Object>of("libelle", libelle, "ville", ville.getNomVille()), utilisateurConnecte());
        log.info("Lieu créé : {} ({})", libelle, ville.getNomVille());
        return ResponseEntity.ok(toDTO(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un lieu")
    public ResponseEntity<LieuDTO> mettreAJour(@PathVariable Integer id, @Valid @RequestBody LieuRequest request) {
        Lieu lieu = lieuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lieu non trouvé avec l'id : " + id));
        String libelle = request.getLibelle() == null ? "" : request.getLibelle().trim();
        if (libelle.isEmpty()) {
            throw new RuntimeException("Le libellé du lieu est obligatoire");
        }
        Ville ville = villeRepository.findById(request.getIdVille())
                .orElseThrow(() -> new RuntimeException("Ville non trouvée avec l'id : " + request.getIdVille()));
        lieuRepository.findByVilleIdVilleAndLibelleIgnoreCase(request.getIdVille(), libelle)
                .filter(l -> !l.getIdLieu().equals(id))
                .ifPresent(l -> {
                    throw new RuntimeException("Le lieu « " + libelle + " » existe déjà dans la ville " + ville.getNomVille());
                });
        Map<String, Object> anciennes = Map.<String, Object>of(
                "libelle", lieu.getLibelle(),
                "ville", lieu.getVille() != null ? lieu.getVille().getNomVille() : "");
        lieu.setLibelle(libelle);
        lieu.setVille(ville);
        Lieu saved = lieuRepository.save(lieu);
        auditService.enregistrer("lieu", id.toString(), "UPDATE",
                anciennes, Map.<String, Object>of("libelle", libelle, "ville", ville.getNomVille()), utilisateurConnecte());
        return ResponseEntity.ok(toDTO(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un lieu")
    public ResponseEntity<Void> supprimer(@PathVariable Integer id) {
        Lieu lieu = lieuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lieu non trouvé avec l'id : " + id));
        lieuRepository.delete(lieu);
        auditService.enregistrer("lieu", id.toString(), "DELETE",
                Map.<String, Object>of("libelle", lieu.getLibelle()), null, utilisateurConnecte());
        log.info("Lieu supprimé : {}", lieu.getLibelle());
        return ResponseEntity.noContent().build();
    }

    private LieuDTO toDTO(Lieu lieu) {
        return LieuDTO.builder()
                .idLieu(lieu.getIdLieu())
                .libelle(lieu.getLibelle())
                .idVille(lieu.getVille() != null ? lieu.getVille().getIdVille() : null)
                .nomVille(lieu.getVille() != null ? lieu.getVille().getNomVille() : null)
                .build();
    }

    private Utilisateur utilisateurConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getPrincipal())) return null;
        return utilisateurRepository.findByNomUtilisateur(auth.getName()).orElse(null);
    }
}