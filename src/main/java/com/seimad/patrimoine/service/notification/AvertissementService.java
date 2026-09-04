package com.seimad.patrimoine.service.notification;

import com.seimad.patrimoine.dto.notification.AvertissementDTO;
import com.seimad.patrimoine.dto.notification.AvertissementRequest;
import com.seimad.patrimoine.entity.notification.Avertissement;
import com.seimad.patrimoine.entity.notification.Personne;
import com.seimad.patrimoine.entity.referentiel.Parcelle;
import com.seimad.patrimoine.entity.referentiel.TitreFoncier;
import com.seimad.patrimoine.repository.notification.AvertissementRepository;
import com.seimad.patrimoine.repository.notification.PersonneRepository;
import com.seimad.patrimoine.repository.referentiel.ParcelleRepository;
import com.seimad.patrimoine.repository.referentiel.TitreFoncierRepository;
import com.seimad.patrimoine.service.dossier.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AvertissementService {

    private final AvertissementRepository avertissementRepository;
    private final PersonneRepository personneRepository;
    private final ParcelleRepository parcelleRepository;
    private final TitreFoncierRepository titreFoncierRepository;
    private final AuditService auditService;

    // ── CRUD ──

    @Transactional(readOnly = true)
    public Page<AvertissementDTO> lister(Pageable pageable) {
        return avertissementRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<AvertissementDTO> rechercher(String search, Pageable pageable) {
        return avertissementRepository.search(search, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public AvertissementDTO trouverParId(UUID id) {
        Avertissement avertissement = avertissementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avertissement non trouvé avec l'id : " + id));
        return toDTO(avertissement);
    }

    @Transactional
    public AvertissementDTO creer(AvertissementRequest request) {
        Personne personne = personneRepository.findById(request.getIdPersonne())
                .orElseThrow(() -> new RuntimeException("Personne non trouvée avec l'id : " + request.getIdPersonne()));
        TitreFoncier titreFoncier = titreFoncierRepository.findById(request.getIdTitreFoncier())
                .orElseThrow(() -> new RuntimeException("Titre foncier non trouvé"));
        Parcelle parcelle = parcelleRepository.findById(request.getIdParcelle())
                .orElseThrow(() -> new RuntimeException("Parcelle non trouvée"));

        Avertissement avertissement = Avertissement.builder()
                .idAvertissement(UUID.randomUUID())
                .dateAvertissement(request.getDateAvertissement())
                .annee(request.getAnnee())
                .informationsOccupants(request.getInformationsOccupants())
                .constats(request.getConstats())
                .actionsEntreprises(request.getActionsEntreprises())
                .aFaire(request.getAFaire())
                .mission(request.getMission())
                .personne(personne)
                .titreFoncier(titreFoncier)
                .parcelle(parcelle)
                .build();
        Avertissement saved = avertissementRepository.save(avertissement);

        // ── Enregistrer l'audit de création ──
        auditService.enregistrer(
                "avertissement",
                saved.getIdAvertissement().toString(),
                "CREATE",
                null,
                valeursAudit(saved)
        );

        return toDTO(saved);
    }

    @Transactional
    public AvertissementDTO mettreAJour(UUID id, AvertissementRequest request) {
        Avertissement avertissement = avertissementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avertissement non trouvé avec l'id : " + id));

        // ── Anciennes valeurs AVANT modification ──
        Map<String, Object> anciennesValeurs = valeursAudit(avertissement);

        Personne personne = personneRepository.findById(request.getIdPersonne())
                .orElseThrow(() -> new RuntimeException("Personne non trouvée avec l'id : " + request.getIdPersonne()));
        TitreFoncier titreFoncier = titreFoncierRepository.findById(request.getIdTitreFoncier())
                .orElseThrow(() -> new RuntimeException("Titre foncier non trouvé"));
        Parcelle parcelle = parcelleRepository.findById(request.getIdParcelle())
                .orElseThrow(() -> new RuntimeException("Parcelle non trouvée"));

        avertissement.setDateAvertissement(request.getDateAvertissement());
        avertissement.setAnnee(request.getAnnee());
        avertissement.setInformationsOccupants(request.getInformationsOccupants());
        avertissement.setConstats(request.getConstats());
        avertissement.setActionsEntreprises(request.getActionsEntreprises());
        avertissement.setAFaire(request.getAFaire());
        avertissement.setMission(request.getMission());
        avertissement.setPersonne(personne);
        avertissement.setTitreFoncier(titreFoncier);
        avertissement.setParcelle(parcelle);

        Avertissement saved = avertissementRepository.save(avertissement);

        // ── Enregistrer l'audit de modification ──
        auditService.enregistrer(
                "avertissement",
                id.toString(),
                "UPDATE",
                anciennesValeurs,
                valeursAudit(saved)
        );

        return toDTO(saved);
    }

    @Transactional
    public void supprimer(UUID id) {
        Avertissement avertissement = avertissementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avertissement non trouvé avec l'id : " + id));

        // ── Anciennes valeurs AVANT suppression ──
        Map<String, Object> anciennesValeurs = valeursAudit(avertissement);

        // ── Enregistrer l'audit de suppression ──
        auditService.enregistrer(
                "avertissement",
                id.toString(),
                "DELETE",
                anciennesValeurs,
                null
        );

        avertissementRepository.deleteById(id);
    }

    // ── Recherches par critère ──

    @Transactional(readOnly = true)
    public List<AvertissementDTO> listerParPersonne(Integer idPersonne) {
        return avertissementRepository.findByPersonneIdPersonne(idPersonne).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AvertissementDTO> listerParAnnee(Integer annee) {
        return avertissementRepository.findByAnnee(annee).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AvertissementDTO> listerParTitreFoncier(UUID idTitreFoncier) {
        return avertissementRepository.findByTitreFoncierIdTitreFoncier(idTitreFoncier).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AvertissementDTO> listerParParcelle(UUID idParcelle) {
        return avertissementRepository.findByParcelleIdParcelle(idParcelle).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── Statistiques ──

    @Transactional(readOnly = true)
    public long compterParAnnee(Integer annee) {
        return avertissementRepository.countByAnnee(annee);
    }

    // ── Helpers ──

    /**
     * Construit la carte des valeurs (audit) d'un avertissement :
     * champs métier + libellés lisibles (n° titre, n° lot, personne).
     */
    private Map<String, Object> valeursAudit(Avertissement a) {
        Map<String, Object> valeurs = new LinkedHashMap<>();
        valeurs.put("dateAvertissement", a.getDateAvertissement());
        valeurs.put("annee", a.getAnnee());
        valeurs.put("informationsOccupants", a.getInformationsOccupants());
        valeurs.put("constats", a.getConstats());
        valeurs.put("actionsEntreprises", a.getActionsEntreprises());
        valeurs.put("aFaire", a.getAFaire());
        valeurs.put("mission", a.getMission());
        valeurs.put("numeroTitre", a.getTitreFoncier() != null ? a.getTitreFoncier().getNumero() : null);
        valeurs.put("numeroLot", a.getParcelle() != null ? a.getParcelle().getNumeroLot() : null);

        Personne p = a.getPersonne();
        valeurs.put("personne", p != null ? p.getNom() : null);
        return valeurs;
    }

    private AvertissementDTO toDTO(Avertissement a) {
        return AvertissementDTO.builder()
                .idAvertissement(a.getIdAvertissement())
                .dateAvertissement(a.getDateAvertissement())
                .annee(a.getAnnee())
                .informationsOccupants(a.getInformationsOccupants())
                .constats(a.getConstats())
                .actionsEntreprises(a.getActionsEntreprises())
                .aFaire(a.getAFaire())
                .mission(a.getMission())
                .createdAt(a.getCreatedAt())
                .idParcelle(a.getParcelle() != null ? a.getParcelle().getIdParcelle() : null)
                .numeroLot(a.getParcelle() != null ? a.getParcelle().getNumeroLot() : null)
                .idTitreFoncier(a.getTitreFoncier() != null ? a.getTitreFoncier().getIdTitreFoncier() : null)
                .numeroTitre(a.getTitreFoncier() != null ? a.getTitreFoncier().getNumero() : null)
                .idPersonne(a.getPersonne() != null ? a.getPersonne().getIdPersonne() : null)
                .nomPersonne(a.getPersonne() != null ? a.getPersonne().getNom() : null)
                .build();
    }
}
