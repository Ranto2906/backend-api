package com.seimad.patrimoine.service.referentiel;

import com.seimad.patrimoine.dto.referentiel.PrixM2DTO;
import com.seimad.patrimoine.dto.referentiel.PrixM2Request;
import com.seimad.patrimoine.entity.referentiel.PrixM2;
import com.seimad.patrimoine.entity.referentiel.Ville;
import com.seimad.patrimoine.repository.referentiel.PrixM2Repository;
import com.seimad.patrimoine.repository.referentiel.VilleRepository;
import com.seimad.patrimoine.service.dossier.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PrixM2Service {

    private final PrixM2Repository prixM2Repository;
    private final VilleRepository villeRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<PrixM2DTO> lister(Pageable pageable) {
        return prixM2Repository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<PrixM2DTO> listerParVille(Integer idVille) {
        return prixM2Repository.findByVilleIdVilleOrderByDateMajDesc(idVille)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PrixM2DTO trouverParId(Integer id) {
        PrixM2 prix = prixM2Repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prix non trouvé avec l'id : " + id));
        return toDTO(prix);
    }

    @Transactional
    public PrixM2DTO creer(PrixM2Request request) {
        Ville ville = villeRepository.findById(request.getIdVille())
                .orElseThrow(() -> new RuntimeException("Ville non trouvée avec l'id : " + request.getIdVille()));
        PrixM2 prix = PrixM2.builder()
                .prixTtc(request.getPrixTtc())
                .prixHt(request.getPrixHt())
                .observation(request.getObservation())
                .dateMaj(request.getDateMaj() != null ? LocalDate.parse(request.getDateMaj()) : LocalDate.now())
                .ville(ville)
                .build();
        PrixM2 saved = prixM2Repository.save(prix);

        // ── Audit ──
        Map<String, Object> nouvellesValeurs = new LinkedHashMap<>();
        nouvellesValeurs.put("prixTtc", saved.getPrixTtc());
        nouvellesValeurs.put("prixHt", saved.getPrixHt());
        nouvellesValeurs.put("observation", saved.getObservation());
        nouvellesValeurs.put("ville", ville.getNomVille());
        auditService.enregistrer("prix_m2", String.valueOf(saved.getIdPrix()), "CREATE", null, nouvellesValeurs);

        return toDTO(saved);
    }

    @Transactional
    public PrixM2DTO mettreAJour(Integer id, PrixM2Request request) {
        PrixM2 prix = prixM2Repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prix non trouvé avec l'id : " + id));
        Ville ville = villeRepository.findById(request.getIdVille())
                .orElseThrow(() -> new RuntimeException("Ville non trouvée avec l'id : " + request.getIdVille()));

        // ── Anciennes valeurs AVANT modification ──
        Map<String, Object> anciennesValeurs = new LinkedHashMap<>();
        anciennesValeurs.put("prixTtc", prix.getPrixTtc());
        anciennesValeurs.put("prixHt", prix.getPrixHt());
        anciennesValeurs.put("observation", prix.getObservation());
        anciennesValeurs.put("ville", prix.getVille() != null ? prix.getVille().getNomVille() : null);

        // ── Appliquer les nouvelles valeurs ──
        prix.setPrixTtc(request.getPrixTtc());
        prix.setPrixHt(request.getPrixHt());
        prix.setObservation(request.getObservation());
        prix.setDateMaj(request.getDateMaj() != null ? LocalDate.parse(request.getDateMaj()) : LocalDate.now());
        prix.setVille(ville);
        PrixM2 saved = prixM2Repository.save(prix);

        // ── Nouvelles valeurs + Audit ──
        Map<String, Object> nouvellesValeurs = new LinkedHashMap<>();
        nouvellesValeurs.put("prixTtc", saved.getPrixTtc());
        nouvellesValeurs.put("prixHt", saved.getPrixHt());
        nouvellesValeurs.put("observation", saved.getObservation());
        nouvellesValeurs.put("ville", ville.getNomVille());
        auditService.enregistrer("prix_m2", String.valueOf(id), "UPDATE", anciennesValeurs, nouvellesValeurs);

        return toDTO(saved);
    }

    @Transactional
    public void supprimer(Integer id) {
        // ── Anciennes valeurs AVANT suppression ──
        PrixM2 prix = prixM2Repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prix non trouvé avec l'id : " + id));
        Map<String, Object> anciennesValeurs = new LinkedHashMap<>();
        anciennesValeurs.put("prixTtc", prix.getPrixTtc());
        anciennesValeurs.put("prixHt", prix.getPrixHt());
        anciennesValeurs.put("observation", prix.getObservation());
        anciennesValeurs.put("ville", prix.getVille() != null ? prix.getVille().getNomVille() : null);

        prixM2Repository.deleteById(id);

        // ── Audit ──
        auditService.enregistrer("prix_m2", String.valueOf(id), "DELETE", anciennesValeurs, null);
    }

    private PrixM2DTO toDTO(PrixM2 p) {
        return PrixM2DTO.builder()
                .idPrix(p.getIdPrix())
                .prixTtc(p.getPrixTtc())
                .prixHt(p.getPrixHt())
                .observation(p.getObservation())
                .dateMaj(p.getDateMaj() != null ? p.getDateMaj().toString() : null)
                .idVille(p.getVille() != null ? p.getVille().getIdVille() : null)
                .nomVille(p.getVille() != null ? p.getVille().getNomVille() : null)
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null)
                .build();
    }
}
