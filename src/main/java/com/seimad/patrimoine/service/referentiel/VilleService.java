package com.seimad.patrimoine.service.referentiel;

import com.seimad.patrimoine.dto.referentiel.VilleDTO;
import com.seimad.patrimoine.dto.referentiel.VilleRequest;
import com.seimad.patrimoine.entity.referentiel.Ville;
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
public class VilleService {

    private final VilleRepository villeRepository;

    @Transactional(readOnly = true)
    public Page<VilleDTO> lister(Pageable pageable) {
        return villeRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<VilleDTO> listerToutes() {
        return villeRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VilleDTO> rechercher(String search) {
        return villeRepository.search(search).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VilleDTO trouverParId(Integer id) {
        Ville ville = villeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ville non trouvée avec l'id : " + id));
        return toDTO(ville);
    }

    @Transactional
    public VilleDTO creer(VilleRequest request) {
        if (villeRepository.existsByNomVille(request.getNomVille())) {
            throw new RuntimeException("Une ville avec ce nom existe déjà");
        }
        Ville ville = Ville.builder()
                .nomVille(request.getNomVille())
                .build();
        return toDTO(villeRepository.save(ville));
    }

    @Transactional
    public VilleDTO mettreAJour(Integer id, VilleRequest request) {
        Ville ville = villeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ville non trouvée avec l'id : " + id));
        ville.setNomVille(request.getNomVille());
        ville.setUpdatedAt(LocalDateTime.now());
        return toDTO(villeRepository.save(ville));
    }

    @Transactional
    public void supprimer(Integer id) {
        villeRepository.deleteById(id);
    }

    private VilleDTO toDTO(Ville v) {
        return VilleDTO.builder()
                .idVille(v.getIdVille())
                .nomVille(v.getNomVille())
                .createdAt(v.getCreatedAt() != null ? v.getCreatedAt().toString() : null)
                .updatedAt(v.getUpdatedAt() != null ? v.getUpdatedAt().toString() : null)
                .build();
    }
}
