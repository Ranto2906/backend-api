package com.seimad.patrimoine.service.referentiel;

import com.seimad.patrimoine.dto.referentiel.ParcelleDTO;
import com.seimad.patrimoine.dto.referentiel.ParcelleRequest;
import com.seimad.patrimoine.entity.referentiel.Parcelle;
import com.seimad.patrimoine.entity.referentiel.Propriete;
import com.seimad.patrimoine.repository.referentiel.ParcelleRepository;
import com.seimad.patrimoine.repository.referentiel.ProprieteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParcelleService {

    private final ParcelleRepository parcelleRepository;
    private final ProprieteRepository proprieteRepository;

    @Transactional(readOnly = true)
    public Page<ParcelleDTO> lister(Pageable pageable) {
        return parcelleRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<ParcelleDTO> listerParPropriete(Integer idPropriete) {
        return parcelleRepository.findByProprieteIdPropriete(idPropriete)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ParcelleDTO trouverParId(UUID id) {
        Parcelle parcelle = parcelleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parcelle non trouvée avec l'id : " + id));
        return toDTO(parcelle);
    }

    @Transactional
    public ParcelleDTO creer(ParcelleRequest request) {
        Propriete tf = proprieteRepository.findById(request.getIdPropriete())
                .orElseThrow(() -> new RuntimeException("Titre foncier non trouvé avec l'id : " + request.getIdPropriete()));
        Parcelle parcelle = Parcelle.builder()
                .idParcelle(request.getIdParcelle() != null ? UUID.fromString(request.getIdParcelle()) : UUID.randomUUID())
                .numeroLot(request.getNumeroLot())
                .superficieM2(request.getSuperficieM2())
                .observation(request.getObservation())
                .propriete(tf)
                .build();
        return toDTO(parcelleRepository.save(parcelle));
    }

    @Transactional
    public ParcelleDTO mettreAJour(UUID id, ParcelleRequest request) {
        Parcelle parcelle = parcelleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parcelle non trouvée avec l'id : " + id));
        Propriete tf = proprieteRepository.findById(request.getIdPropriete())
                .orElseThrow(() -> new RuntimeException("Titre foncier non trouvé avec l'id : " + request.getIdPropriete()));
        parcelle.setNumeroLot(request.getNumeroLot());
        parcelle.setSuperficieM2(request.getSuperficieM2());
        parcelle.setObservation(request.getObservation());
        parcelle.setPropriete(tf);
        parcelle.setUpdatedAt(LocalDateTime.now());
        return toDTO(parcelleRepository.save(parcelle));
    }

    @Transactional
    public void supprimer(UUID id) {
        parcelleRepository.deleteById(id);
    }

    private ParcelleDTO toDTO(Parcelle p) {
        return ParcelleDTO.builder()
                .idParcelle(p.getIdParcelle().toString())
                .numeroLot(p.getNumeroLot())
                .superficieM2(p.getSuperficieM2())
                .observation(p.getObservation())
                .idPropriete(p.getPropriete() != null ? p.getPropriete().getIdPropriete() : null)
                .numeroPropriete(p.getPropriete() != null ? p.getPropriete().getNumero() : null)
                .nomPropriete(p.getPropriete() != null ? p.getPropriete().getNom() : null)
                .zonePropriete(p.getPropriete() != null ? p.getPropriete().getZone() : null)
                .localisationPropriete(p.getPropriete() != null ? p.getPropriete().getLocalisation() : null)
                .nomVille(p.getPropriete() != null && p.getPropriete().getVille() != null
                        ? p.getPropriete().getVille().getNomVille() : null)
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null)
                .updatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null)
                .build();
    }
}
