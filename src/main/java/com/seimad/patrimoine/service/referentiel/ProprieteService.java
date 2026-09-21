package com.seimad.patrimoine.service.referentiel;

import com.seimad.patrimoine.dto.carte.GeometrieRequest;
import com.seimad.patrimoine.dto.referentiel.ProprieteDTO;
import com.seimad.patrimoine.dto.referentiel.ProprieteRequest;
import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.referentiel.Propriete;
import com.seimad.patrimoine.entity.referentiel.Lieu;
import com.seimad.patrimoine.repository.auth.UtilisateurRepository;
import com.seimad.patrimoine.repository.carte.GeometrieRepository;
import com.seimad.patrimoine.repository.referentiel.ProprieteRepository;
import com.seimad.patrimoine.repository.referentiel.LieuRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProprieteService {

    private final ProprieteRepository proprieteRepository;
    private final LieuRepository lieuRepository;
    private final GeometrieRepository geometrieRepository;
    private final UtilisateurRepository utilisateurRepository;

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

    @Transactional
    public ProprieteDTO creer(ProprieteRequest request) {
        Lieu lieu = lieuRepository.findById(request.getIdLieu())
            .orElseThrow(() -> new RuntimeException("Lieu non trouvé avec l'id : " + request.getIdLieu()));
        Propriete propriete = Propriete.builder()
                .nom(request.getNom())
                .numero(request.getNumero())
                .zone(request.getZone())
                .localisation(request.getLocalisation())
                .superficieTotale(request.getSuperficieTotale())
                .lieu(lieu)
                .build();
        Propriete saved = proprieteRepository.save(propriete);
        enregistrerGeometrie(request.getGeometrie(), saved.getIdPropriete());
        return toDTO(saved);
    }

    @Transactional
    public ProprieteDTO mettreAJour(Integer id, ProprieteRequest request) {
        Propriete propriete = proprieteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propriété non trouvée avec l'id : " + id));
        Lieu lieu = lieuRepository.findById(request.getIdLieu())
            .orElseThrow(() -> new RuntimeException("Lieu non trouvé avec l'id : " + request.getIdLieu()));
        propriete.setNom(request.getNom());
        propriete.setNumero(request.getNumero());
        propriete.setZone(request.getZone());
        propriete.setLocalisation(request.getLocalisation());
        propriete.setSuperficieTotale(request.getSuperficieTotale());
        propriete.setLieu(lieu);
        propriete.setUpdatedAt(LocalDateTime.now());
        Propriete saved = proprieteRepository.save(propriete);
        enregistrerGeometrie(request.getGeometrie(), saved.getIdPropriete());
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
                .idLieu(p.getLieu() != null ? p.getLieu().getIdLieu() : null)
                .libelleLieu(p.getLieu() != null ? p.getLieu().getLibelle() : null)
                .idVille(p.getLieu() != null && p.getLieu().getVille() != null ? p.getLieu().getVille().getIdVille() : null)
                .nomVille(p.getLieu() != null && p.getLieu().getVille() != null ? p.getLieu().getVille().getNomVille() : null)
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null)
                .updatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null)
                .build();
    }
}
