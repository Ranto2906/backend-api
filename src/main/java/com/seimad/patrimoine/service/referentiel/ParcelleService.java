package com.seimad.patrimoine.service.referentiel;

import com.seimad.patrimoine.dto.referentiel.ParcelleDTO;
import com.seimad.patrimoine.dto.referentiel.ParcelleRequest;
import com.seimad.patrimoine.entity.referentiel.Parcelle;
import com.seimad.patrimoine.entity.referentiel.TitreFoncier;
import com.seimad.patrimoine.repository.referentiel.ParcelleRepository;
import com.seimad.patrimoine.repository.referentiel.TitreFoncierRepository;
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
    private final TitreFoncierRepository titreFoncierRepository;

    @Transactional(readOnly = true)
    public Page<ParcelleDTO> lister(Pageable pageable) {
        return parcelleRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<ParcelleDTO> listerParTitreFoncier(UUID idTitreFoncier) {
        return parcelleRepository.findByTitreFoncierIdTitreFoncier(idTitreFoncier)
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
        TitreFoncier tf = titreFoncierRepository.findById(UUID.fromString(request.getIdTitreFoncier()))
                .orElseThrow(() -> new RuntimeException("Titre foncier non trouvé avec l'id : " + request.getIdTitreFoncier()));
        Parcelle parcelle = Parcelle.builder()
                .idParcelle(request.getIdParcelle() != null ? UUID.fromString(request.getIdParcelle()) : UUID.randomUUID())
                .numeroLot(request.getNumeroLot())
                .superficieM2(request.getSuperficieM2())
                .observation(request.getObservation())
                .titreFoncier(tf)
                .build();
        return toDTO(parcelleRepository.save(parcelle));
    }

    @Transactional
    public ParcelleDTO mettreAJour(UUID id, ParcelleRequest request) {
        Parcelle parcelle = parcelleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parcelle non trouvée avec l'id : " + id));
        TitreFoncier tf = titreFoncierRepository.findById(UUID.fromString(request.getIdTitreFoncier()))
                .orElseThrow(() -> new RuntimeException("Titre foncier non trouvé avec l'id : " + request.getIdTitreFoncier()));
        parcelle.setNumeroLot(request.getNumeroLot());
        parcelle.setSuperficieM2(request.getSuperficieM2());
        parcelle.setObservation(request.getObservation());
        parcelle.setTitreFoncier(tf);
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
                .idTitreFoncier(p.getTitreFoncier() != null ? p.getTitreFoncier().getIdTitreFoncier().toString() : null)
                .numeroTitreFoncier(p.getTitreFoncier() != null ? p.getTitreFoncier().getNumero() : null)
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null)
                .updatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null)
                .build();
    }
}
