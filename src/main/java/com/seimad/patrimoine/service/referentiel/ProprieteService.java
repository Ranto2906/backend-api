package com.seimad.patrimoine.service.referentiel;

import com.seimad.patrimoine.dto.carte.GeometrieRequest;
import com.seimad.patrimoine.dto.referentiel.ProprieteDTO;
import com.seimad.patrimoine.dto.referentiel.ProprieteOccupationDTO;
import com.seimad.patrimoine.dto.referentiel.ProprieteRequest;
import com.seimad.patrimoine.dto.referentiel.TypeProprieteDTO;
import com.seimad.patrimoine.entity.referentiel.TypePropriete;
import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.dossier.DossierDemandeur;
import com.seimad.patrimoine.entity.dossier.DossierParcelle;
import com.seimad.patrimoine.entity.referentiel.Propriete;
import com.seimad.patrimoine.entity.referentiel.Lieu;
import com.seimad.patrimoine.repository.auth.UtilisateurRepository;
import com.seimad.patrimoine.repository.carte.GeometrieRepository;
import com.seimad.patrimoine.repository.dossier.DossierDemandeurRepository;
import com.seimad.patrimoine.repository.dossier.DossierParcelleRepository;
import com.seimad.patrimoine.repository.referentiel.ProprieteRepository;
import com.seimad.patrimoine.repository.referentiel.LieuRepository;
import com.seimad.patrimoine.repository.referentiel.TypeProprieteRepository;
import com.seimad.patrimoine.service.dossier.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProprieteService {

    private final ProprieteRepository proprieteRepository;
    private final DossierParcelleRepository dossierParcelleRepository;
    private final DossierDemandeurRepository dossierDemandeurRepository;
    private final LieuRepository lieuRepository;
    private final TypeProprieteRepository typeProprieteRepository;
    private final GeometrieRepository geometrieRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<ProprieteDTO> lister(Pageable pageable) {
        return proprieteRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<ProprieteDTO> rechercher(String search) {
        return proprieteRepository.search(search).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProprieteDTO> listerParVille(Integer idVille) {
        return proprieteRepository.findByLieuVilleIdVille(idVille).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProprieteDTO trouverParId(Integer id) {
        Propriete propriete = proprieteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propriété non trouvée avec l'id : " + id));
        return toDTO(propriete);
    }

    /**
     * Dossiers rattachés à une propriété (N° dossier, demandeur, superficie)
     * et calcul de la superficie restante.
     *
     * @param id            propriété concernée
     * @param exclureDossier dossier à exclure du calcul (édition : ses propres
     *                       lignes ne réduisent pas le reste disponible)
     */
    @Transactional(readOnly = true)
    public ProprieteOccupationDTO occupation(Integer id, Integer exclureDossier) {
        Propriete propriete = proprieteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propriété non trouvée avec l'id : " + id));

        List<DossierParcelle> lignes = dossierParcelleRepository
                .findByPropriete_IdProprieteOrderByDateAjoutAsc(id).stream()
                .filter(dp -> dp.getDossier() != null
                        && (exclureDossier == null
                        || !exclureDossier.equals(dp.getDossier().getIdDossier())))
                .collect(Collectors.toList());

        // ── Agrégation par dossier : numéro + superficie occupée ──
        Map<Integer, ProprieteOccupationDTO.DossierLieDTO> parDossier = new LinkedHashMap<>();
        for (DossierParcelle dp : lignes) {
            Integer idDossier = dp.getDossier().getIdDossier();
            ProprieteOccupationDTO.DossierLieDTO ligne = parDossier.computeIfAbsent(idDossier, k ->
                    ProprieteOccupationDTO.DossierLieDTO.builder()
                            .idDossier(idDossier)
                            .numeroDossier(dp.getDossier().getNumeroDossier())
                            .demandeur(null)
                            .superficieM2(BigDecimal.ZERO)
                            .build());
            if (dp.getSuperficieM2() != null) {
                ligne.setSuperficieM2(ligne.getSuperficieM2().add(dp.getSuperficieM2()));
            }
        }

        // ── Demandeur principal de chaque dossier ──
        if (!parDossier.isEmpty()) {
            Set<Integer> avecPrincipal = new HashSet<>();
            for (DossierDemandeur dd : dossierDemandeurRepository.findByIdDossierIn(parDossier.keySet())) {
                ProprieteOccupationDTO.DossierLieDTO ligne = parDossier.get(dd.getIdDossier());
                if (ligne == null || dd.getPersonne() == null || dd.getPersonne().getNom() == null) continue;
                boolean principal = "Principal".equalsIgnoreCase(dd.getRole());
                if (principal && avecPrincipal.add(dd.getIdDossier())) {
                    ligne.setDemandeur(dd.getPersonne().getNom());
                } else {
                    ligne.setDemandeur(ligne.getDemandeur() != null ? ligne.getDemandeur()
                            : dd.getPersonne().getNom());
                }
            }
        }

        List<ProprieteOccupationDTO.DossierLieDTO> dossiers = new ArrayList<>(parDossier.values());
        dossiers.sort((a, b) -> String.valueOf(a.getNumeroDossier())
                .compareToIgnoreCase(String.valueOf(b.getNumeroDossier())));

        BigDecimal utilisee = dossiers.stream()
                .map(ProprieteOccupationDTO.DossierLieDTO::getSuperficieM2)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totale = propriete.getSuperficieTotale();
        BigDecimal restante = totale != null ? totale.subtract(utilisee) : null;
        boolean disponible = restante == null || restante.signum() > 0;

        return ProprieteOccupationDTO.builder()
                .idPropriete(id)
                .superficieTotale(totale)
                .superficieUtilisee(utilisee)
                .superficieRestante(restante)
                .disponible(disponible)
                .dossiers(dossiers)
                .build();
    }

    @Transactional
    public ProprieteDTO creer(ProprieteRequest request) {
        Lieu lieu = lieuRepository.findById(request.getIdLieu())
            .orElseThrow(() -> new RuntimeException("Lieu non trouvé avec l'id : " + request.getIdLieu()));
        TypePropriete type = typeProprieteRepository.findById(request.getIdTypePropriete())
            .orElseThrow(() -> new RuntimeException(
                "Type de propriété non trouvé avec l'id : " + request.getIdTypePropriete()));
        Propriete propriete = Propriete.builder()
                .nom(request.getNom())
                .numero(request.getNumero())
                .zone(request.getZone())
                .localisation(request.getLocalisation())
                .superficieTotale(request.getSuperficieTotale())
                .typePropriete(type)
                .lieu(lieu)
                .build();
        Propriete saved = proprieteRepository.save(propriete);
        enregistrerGeometrie(request.getGeometrie(), saved.getIdPropriete());
        auditService.enregistrer(
                "propriete", saved.getIdPropriete().toString(), "CREATE", null,
                snapshot(saved), utilisateurConnecte());
        return toDTO(saved);
    }

    @Transactional
    public ProprieteDTO mettreAJour(Integer id, ProprieteRequest request) {
        Propriete propriete = proprieteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propriété non trouvée avec l'id : " + id));
        Lieu lieu = lieuRepository.findById(request.getIdLieu())
            .orElseThrow(() -> new RuntimeException("Lieu non trouvé avec l'id : " + request.getIdLieu()));
        TypePropriete type = typeProprieteRepository.findById(request.getIdTypePropriete())
            .orElseThrow(() -> new RuntimeException(
                "Type de propriété non trouvé avec l'id : " + request.getIdTypePropriete()));

        Map<String, Object> anciennes = snapshot(propriete);
        propriete.setNom(request.getNom());
        propriete.setNumero(request.getNumero());
        propriete.setZone(request.getZone());
        propriete.setLocalisation(request.getLocalisation());
        propriete.setSuperficieTotale(request.getSuperficieTotale());
        propriete.setTypePropriete(type);
        propriete.setLieu(lieu);
        propriete.setUpdatedAt(LocalDateTime.now());
        Propriete saved = proprieteRepository.save(propriete);

        // Sémaphore : si les données ont réellement changé, on trace l'audit.
        Map<String, Object> nouvelles = snapshot(saved);
        if (!anciennes.equals(nouvelles)) {
            enregistrerGeometrie(request.getGeometrie(), saved.getIdPropriete());
            auditService.enregistrer(
                    "propriete", id.toString(), "UPDATE", anciennes, nouvelles,
                    utilisateurConnecte());
        }
        return toDTO(saved);
    }

    @Transactional
    public void supprimer(Integer id) {
        geometrieRepository.supprimerParEntite("propriete", integerToUuid(id));
        proprieteRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> geometrie(Integer id) {
        return geometrieRepository.projeterParEntite("propriete", integerToUuid(id));
    }

    // ─────────────────────────────────────────────────────────────
    // Géométrie (PostGIS)
    // ─────────────────────────────────────────────────────────────

    /**
     * Convertit un ID Integer en UUID pour le stockage dans geometrie.entite_id.
     * Format : 00000000-0000-0000-0000-{id:012d}
     */
    private UUID integerToUuid(Integer id) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", id));
    }

    private void enregistrerGeometrie(GeometrieRequest geometrie, Integer idPropriete) {
        if (geometrie == null || geometrie.getGeojson() == null || geometrie.getGeojson().isBlank()) {
            return;
        }
        Utilisateur utilisateur = utilisateurConnecte();
        if (utilisateur == null) {
            log.warn("Géométrie ignorée pour la propriété {} : utilisateur non résolu", idPropriete);
            return;
        }
        String type = geometrie.getTypeGeometrie() != null ? geometrie.getTypeGeometrie() : "Point";
        String source = geometrie.getSource() != null && !geometrie.getSource().isBlank()
                ? geometrie.getSource() : "Point carte";
        BigDecimal precisionM = geometrie.getPrecisionM() != null
                ? BigDecimal.valueOf(geometrie.getPrecisionM()) : null;
        geometrieRepository.upsert("propriete", integerToUuid(idPropriete), type, geometrie.getGeojson(),
                precisionM, source, utilisateur.getIdUtilisateur());
        log.info("Géométrie {} enregistrée pour la propriété {}", type, idPropriete);
    }

    private Utilisateur utilisateurConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getPrincipal())) return null;
        return utilisateurRepository.findByNomUtilisateur(auth.getName()).orElse(null);
    }

    private ProprieteDTO toDTO(Propriete p) {
        return ProprieteDTO.builder()
                .idPropriete(p.getIdPropriete())
                .nom(p.getNom())
                .numero(p.getNumero())
                .zone(p.getZone())
                .localisation(p.getLocalisation())
                .superficieTotale(p.getSuperficieTotale())
                .typePropriete(typeProprieteDTO(p.getTypePropriete()))
                .idLieu(p.getLieu() != null ? p.getLieu().getIdLieu() : null)
                .libelleLieu(p.getLieu() != null ? p.getLieu().getLibelle() : null)
                .idVille(p.getLieu() != null && p.getLieu().getVille() != null ? p.getLieu().getVille().getIdVille() : null)
                .nomVille(p.getLieu() != null && p.getLieu().getVille() != null ? p.getLieu().getVille().getNomVille() : null)
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null)
                .updatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null)
                .build();
    }

    private TypeProprieteDTO typeProprieteDTO(TypePropriete type) {
        if (type == null) return null;
        return TypeProprieteDTO.builder()
                .idTypePropriete(type.getIdTypePropriete())
                .libelle(type.getLibelle())
                .build();
    }

    private Map<String, Object> snapshot(Propriete p) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("idPropriete", p.getIdPropriete());
        map.put("nom", p.getNom());
        map.put("numero", p.getNumero());
        map.put("zone", p.getZone());
        map.put("localisation", p.getLocalisation());
        map.put("superficieTotale", p.getSuperficieTotale() != null ? p.getSuperficieTotale().toPlainString() : null);
        map.put("typePropriete", p.getTypePropriete() != null ? p.getTypePropriete().getLibelle() : null);
        map.put("idLieu", p.getLieu() != null ? p.getLieu().getIdLieu() : null);
        map.put("libelleLieu", p.getLieu() != null ? p.getLieu().getLibelle() : null);
        return map;
    }
}
