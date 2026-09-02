package com.seimad.patrimoine.service.referentiel;

import com.seimad.patrimoine.dto.referentiel.ProprieteDTO;
import com.seimad.patrimoine.dto.referentiel.ProprieteRequest;
import com.seimad.patrimoine.entity.referentiel.Propriete;
import com.seimad.patrimoine.entity.referentiel.Ville;
import com.seimad.patrimoine.repository.referentiel.ProprieteRepository;
import com.seimad.patrimoine.repository.referentiel.VilleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProprieteService {

    private final ProprieteRepository proprieteRepository;
    private final VilleRepository villeRepository;

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
        return proprieteRepository.findByVilleIdVille(idVille).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProprieteDTO trouverParId(Integer id) {
        Propriete propriete = proprieteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propriété non trouvée avec l'id : " + id));
        return toDTO(propriete);
    }

    @Transactional
    public ProprieteDTO creer(ProprieteRequest request) {
        Ville ville = villeRepository.findById(request.getIdVille())
                .orElseThrow(() -> new RuntimeException("Ville non trouvée avec l'id : " + request.getIdVille()));
        Propriete propriete = Propriete.builder()
                .nom(request.getNom())
                .ville(ville)
                .build();
        return toDTO(proprieteRepository.save(propriete));
    }

    @Transactional
    public ProprieteDTO mettreAJour(Integer id, ProprieteRequest request) {
        Propriete propriete = proprieteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propriété non trouvée avec l'id : " + id));
        Ville ville = villeRepository.findById(request.getIdVille())
                .orElseThrow(() -> new RuntimeException("Ville non trouvée avec l'id : " + request.getIdVille()));
        propriete.setNom(request.getNom());
        propriete.setVille(ville);
        propriete.setUpdatedAt(LocalDateTime.now());
        return toDTO(proprieteRepository.save(propriete));
    }

    @Transactional
    public void supprimer(Integer id) {
        proprieteRepository.deleteById(id);
    }

    private ProprieteDTO toDTO(Propriete p) {
        return ProprieteDTO.builder()
                .idPropriete(p.getIdPropriete())
                .nom(p.getNom())
                .idVille(p.getVille() != null ? p.getVille().getIdVille() : null)
                .nomVille(p.getVille() != null ? p.getVille().getNomVille() : null)
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null)
                .updatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null)
                .build();
    }
}
