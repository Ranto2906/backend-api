package com.seimad.patrimoine.controller.dossier;

import com.seimad.patrimoine.dto.dossier.DossierDTO;
import com.seimad.patrimoine.dto.dossier.DossierRequest;
import com.seimad.patrimoine.dto.dossier.EtapeRequest;
import com.seimad.patrimoine.dto.dossier.SuiviDossierRequest;
import com.seimad.patrimoine.entity.dossier.Etape;
import com.seimad.patrimoine.entity.dossier.DossierFournie;
import com.seimad.patrimoine.entity.dossier.StatutCouleur;
import com.seimad.patrimoine.entity.dossier.StatutDossier;
import com.seimad.patrimoine.entity.dossier.TypeDossier;
import com.seimad.patrimoine.service.dossier.DossierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Suivi de dossier — demandes, demandeurs, changements d'étape.
 */
@RestController
@RequestMapping("/api/dossiers")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:8100"})
@RequiredArgsConstructor
@Slf4j
public class DossierController {

    private final DossierService dossierService;
    private final com.seimad.patrimoine.repository.dossier.EtapeRepository etapeRepository;
    private final com.seimad.patrimoine.repository.dossier.StatutDossierRepository statutDossierRepository;
    private final com.seimad.patrimoine.repository.dossier.TypeDossierRepository typeDossierRepository;
    private final com.seimad.patrimoine.repository.dossier.DossierFournieRepository dossierFournieRepository;
    private final com.seimad.patrimoine.repository.dossier.StatutCouleurRepository statutCouleurRepository;
    private final com.seimad.patrimoine.repository.referentiel.PrixM2Repository prixM2Repository;
    private final com.seimad.patrimoine.service.referentiel.ProprieteService proprieteService;
    private final com.seimad.patrimoine.service.referentiel.ParcelleService parcelleService;

    // ── GET /api/dossiers/etapes ──
    @GetMapping("/etapes")
    public ResponseEntity<List<Etape>> listerEtapes() {
        return ResponseEntity.ok(etapeRepository.findAllByOrderByOrdreAsc());
    }

    // ── POST /api/dossiers/etapes — configuration du workflow ──
    @PostMapping("/etapes")
    public ResponseEntity<Etape> creerEtape(@Valid @RequestBody EtapeRequest request) {
        log.info("Création d'une étape : {} (ordre={})", request.getLibelle(), request.getOrdre());
        return ResponseEntity.status(HttpStatus.CREATED).body(dossierService.creerEtape(request));
    }

    // ── PUT /api/dossiers/etapes/{id} ──
    @PutMapping("/etapes/{id}")
    public ResponseEntity<Etape> mettreAJourEtape(@PathVariable Integer id,
                                                  @Valid @RequestBody EtapeRequest request) {
        log.info("Mise à jour de l'étape id={}", id);
        return ResponseEntity.ok(dossierService.mettreAJourEtape(id, request));
    }

    // ── DELETE /api/dossiers/etapes/{id} — refusée si utilisée dans un suivi ──
    @DeleteMapping("/etapes/{id}")
    public ResponseEntity<Void> supprimerEtapeReferentiel(@PathVariable Integer id) {
        log.info("Suppression de l'étape id={}", id);
        dossierService.supprimerEtapeReferentiel(id);
        return ResponseEntity.noContent().build();
    }

    // ── GET /api/dossiers/statuts ──
    @GetMapping("/statuts")
    public ResponseEntity<List<StatutDossier>> listerStatuts() {
        return ResponseEntity.ok(statutDossierRepository.findAllByOrderByOrdreAsc());
    }

    // ── GET /api/dossiers/types ──
    @GetMapping("/types")
    public ResponseEntity<List<TypeDossier>> listerTypesDossier() {
        return ResponseEntity.ok(typeDossierRepository.findAll());
    }

    // ── GET /api/dossiers/fournis ──
    @GetMapping("/fournis")
    public ResponseEntity<List<DossierFournie>> listerDossiersFournis() {
        return ResponseEntity.ok(dossierFournieRepository.findAll());
    }

    // ── GET /api/dossiers/statuts-couleur ──
    @GetMapping("/statuts-couleur")
    public ResponseEntity<List<StatutCouleur>> listerStatutsCouleur() {
        return ResponseEntity.ok(statutCouleurRepository.findAll());
    }

    // ── Référentiels pour le formulaire de demande ──
    // Propriétés d'une ville (recherche de propriété)
    @GetMapping("/proprietes")
    public ResponseEntity<List<com.seimad.patrimoine.dto.referentiel.ProprieteDTO>> listerProprietes(
            @RequestParam(required = false) Integer idVille,
            @RequestParam(required = false) String search) {
        List<com.seimad.patrimoine.dto.referentiel.ProprieteDTO> result;
        if (search != null && !search.isBlank()) {
            result = proprieteService.rechercher(search).stream()
                    .filter(p -> idVille == null || idVille.equals(p.getIdVille()))
                    .toList();
        } else if (idVille != null) {
            result = proprieteService.listerParVille(idVille);
        } else {
            result = proprieteService.lister(PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "nom"))).getContent();
        }
        return ResponseEntity.ok(result);
    }

    // Parcelles d'une propriété (liste déroulante liée)
    @GetMapping("/proprietes/{idPropriete}/parcelles")
    public ResponseEntity<List<com.seimad.patrimoine.dto.referentiel.ParcelleDTO>> listerParcellesPropriete(
            @PathVariable Integer idPropriete) {
        return ResponseEntity.ok(parcelleService.listerParPropriete(idPropriete));
    }

    // Dernier prix au m² d'une ville (calcul automatique HT/TTC)
    @GetMapping("/prix-ville/{idVille}")
    public ResponseEntity<Map<String, Object>> dernierPrixVille(@PathVariable Integer idVille) {
        List<com.seimad.patrimoine.entity.referentiel.PrixM2> prix =
                prixM2Repository.findByVilleIdVilleOrderByDateMajDesc(idVille);
        if (prix.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        com.seimad.patrimoine.entity.referentiel.PrixM2 p = prix.get(0);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("idPrix", p.getIdPrix());
        body.put("prixTtc", p.getPrixTtc());
        body.put("prixHt", p.getPrixHt());
        body.put("dateMaj", p.getDateMaj());
        return ResponseEntity.ok(body);
    }

    // ── GET /api/dossiers ──
    @GetMapping
    public ResponseEntity<Page<DossierDTO>> lister(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(dossierService.lister(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateCreation"))));
    }

    /**
     * GET /api/dossiers/search?search=&idStatut=&idVille=&annee=&du=&au=
     * Recherche multi-critères paginée.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<DossierDTO>> rechercher(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer idStatut,
            @RequestParam(required = false) Integer idVille,
            @RequestParam(required = false) Integer annee,
            @RequestParam(name = "du", required = false) LocalDate dateDebut,
            @RequestParam(name = "au", required = false) LocalDate dateFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(dossierService.rechercher(search, idStatut, idVille, annee,
                dateDebut, dateFin, PageRequest.of(page, size)));
    }

    // ── GET /api/dossiers/{id} ──
    @GetMapping("/{id}")
    public ResponseEntity<DossierDTO> trouverParId(@PathVariable Integer id) {
        return ResponseEntity.ok(dossierService.trouverParId(id));
    }

    // ── POST /api/dossiers ──
    @PostMapping
    public ResponseEntity<DossierDTO> creer(@Valid @RequestBody DossierRequest request) {
        log.info("Création du dossier {}", request.getNumeroDossier());
        return ResponseEntity.status(HttpStatus.CREATED).body(dossierService.creer(request));
    }

    // ── PUT /api/dossiers/{id} ──
    @PutMapping("/{id}")
    public ResponseEntity<DossierDTO> mettreAJour(
            @PathVariable Integer id,
            @Valid @RequestBody DossierRequest request) {
        log.info("Mise à jour du dossier id={}", id);
        return ResponseEntity.ok(dossierService.mettreAJour(id, request));
    }

    // ── DELETE /api/dossiers/{id} ──
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Integer id) {
        log.info("Suppression du dossier id={}", id);
        dossierService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/dossiers/{id}/suivi — changement d'étape du suivi.
     * Enregistre (ou met à jour) une étape réalisée et met à jour le statut du dossier.
     */
    @PostMapping("/{id}/suivi")
    public ResponseEntity<DossierDTO.SuiviDossierDTO> enregistrerEtape(
            @PathVariable Integer id,
            @Valid @RequestBody SuiviDossierRequest request) {
        log.info("Changement d'étape dossier id={} → etape={}", id, request.getIdEtape());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dossierService.enregistrerEtape(id, request));
    }

    // ── DELETE /api/dossiers/{id}/suivi/{idSuivi} ──
    @DeleteMapping("/{id}/suivi/{idSuivi}")
    public ResponseEntity<Void> supprimerEtape(@PathVariable Integer id, @PathVariable Integer idSuivi) {
        dossierService.supprimerEtape(id, idSuivi);
        return ResponseEntity.noContent().build();
    }

    // ── GET /api/dossiers/{id}/historique ──
    @GetMapping("/{id}/historique")
    public ResponseEntity<List<DossierDTO.SuiviDossierDTO>> historiqueSuivi(@PathVariable Integer id) {
        return ResponseEntity.ok(dossierService.historiqueSuivi(id));
    }

    // ── GET /api/dossiers/stats ──
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> statistiques() {
        return ResponseEntity.ok(dossierService.statistiques());
    }
}
