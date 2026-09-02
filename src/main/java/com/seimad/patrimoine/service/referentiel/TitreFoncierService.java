package com.seimad.patrimoine.service.referentiel;

import com.seimad.patrimoine.dto.referentiel.TitreFoncierDTO;
import com.seimad.patrimoine.dto.referentiel.TitreFoncierRequest;
import com.seimad.patrimoine.entity.referentiel.Propriete;
import com.seimad.patrimoine.entity.referentiel.TitreFoncier;
import com.seimad.patrimoine.repository.referentiel.ProprieteRepository;
import com.seimad.patrimoine.repository.referentiel.TitreFoncierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TitreFoncierService {

    private final TitreFoncierRepository titreFoncierRepository;
    private final ProprieteRepository proprieteRepository;

    @Transactional(readOnly = true)
    public Page<TitreFoncierDTO> lister(Pageable pageable) {
        return titreFoncierRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public TitreFoncierDTO trouverParId(UUID id) {
        TitreFoncier tf = titreFoncierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Titre foncier non trouvé avec l'id : " + id));
        return toDTO(tf);
    }

    @Transactional(readOnly = true)
    public TitreFoncierDTO trouverParNumero(String numero) {
        TitreFoncier tf = titreFoncierRepository.findByNumero(numero)
                .orElseThrow(() -> new RuntimeException("Titre foncier non trouvé avec le numéro : " + numero));
        return toDTO(tf);
    }

    @Transactional
    public TitreFoncierDTO creer(TitreFoncierRequest request) {
        Propriete propriete = proprieteRepository.findById(request.getIdPropriete())
                .orElseThrow(() -> new RuntimeException("Propriété non trouvée avec l'id : " + request.getIdPropriete()));

        TitreFoncier tf = TitreFoncier.builder()
                .idTitreFoncier(request.getIdTitreFoncier() != null ? UUID.fromString(request.getIdTitreFoncier()) : UUID.randomUUID())
                .numero(request.getNumero())
                .typeTitre(request.getTypeTitre() != null ? request.getTypeTitre() : "Titre Foncier")
                .zone(request.getZone())
                .localisation(request.getLocalisation())
                .superficieTotale(request.getSuperficieTotale())
                .propriete(propriete)
                .build();
        return toDTO(titreFoncierRepository.save(tf));
    }

    @Transactional
    public TitreFoncierDTO mettreAJour(UUID id, TitreFoncierRequest request) {
        TitreFoncier tf = titreFoncierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Titre foncier non trouvé avec l'id : " + id));
        Propriete propriete = proprieteRepository.findById(request.getIdPropriete())
                .orElseThrow(() -> new RuntimeException("Propriété non trouvée avec l'id : " + request.getIdPropriete()));

        tf.setNumero(request.getNumero());
        tf.setTypeTitre(request.getTypeTitre());
        tf.setZone(request.getZone());
        tf.setLocalisation(request.getLocalisation());
        tf.setSuperficieTotale(request.getSuperficieTotale());
        tf.setPropriete(propriete);
        tf.setUpdatedAt(LocalDateTime.now());
        return toDTO(titreFoncierRepository.save(tf));
    }

    @Transactional
    public void supprimer(UUID id) {
        titreFoncierRepository.deleteById(id);
    }

    private TitreFoncierDTO toDTO(TitreFoncier tf) {
        return TitreFoncierDTO.builder()
                .idTitreFoncier(tf.getIdTitreFoncier().toString())
                .numero(tf.getNumero())
                .typeTitre(tf.getTypeTitre())
                .zone(tf.getZone())
                .localisation(tf.getLocalisation())
                .superficieTotale(tf.getSuperficieTotale())
                .idPropriete(tf.getPropriete() != null ? tf.getPropriete().getIdPropriete() : null)
                .nomPropriete(tf.getPropriete() != null ? tf.getPropriete().getNom() : null)
                .createdAt(tf.getCreatedAt() != null ? tf.getCreatedAt().toString() : null)
                .updatedAt(tf.getUpdatedAt() != null ? tf.getUpdatedAt().toString() : null)
                .build();
    }
}
