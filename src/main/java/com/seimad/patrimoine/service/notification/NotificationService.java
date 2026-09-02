package com.seimad.patrimoine.service.notification;

import com.seimad.patrimoine.dto.notification.*;
import com.seimad.patrimoine.entity.notification.NotificationOccupation;
import com.seimad.patrimoine.entity.notification.NotificationPersonne;
import com.seimad.patrimoine.entity.notification.Personne;
import com.seimad.patrimoine.entity.referentiel.Parcelle;
import com.seimad.patrimoine.entity.referentiel.TitreFoncier;
import com.seimad.patrimoine.repository.notification.NotificationOccupationRepository;
import com.seimad.patrimoine.repository.notification.NotificationPersonneRepository;
import com.seimad.patrimoine.repository.notification.PersonneRepository;
import com.seimad.patrimoine.repository.referentiel.ParcelleRepository;
import com.seimad.patrimoine.repository.referentiel.TitreFoncierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationOccupationRepository notificationRepository;
    private final NotificationPersonneRepository notificationPersonneRepository;
    private final PersonneRepository personneRepository;
    private final ParcelleRepository parcelleRepository;
    private final TitreFoncierRepository titreFoncierRepository;

    // ── CRUD ──

    @Transactional(readOnly = true)
    public Page<NotificationOccupationDTO> lister(Pageable pageable) {
        return notificationRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<NotificationOccupationDTO> rechercher(String search, Pageable pageable) {
        return notificationRepository.search(search, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public NotificationOccupationDTO trouverParId(UUID id) {
        NotificationOccupation notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée avec l'id : " + id));
        return toDTO(notification);
    }

    @Transactional
    public NotificationOccupationDTO creer(NotificationOccupationRequest request) {
        TitreFoncier titreFoncier = titreFoncierRepository.findById(request.getIdTitreFoncier())
                .orElseThrow(() -> new RuntimeException("Titre foncier non trouvé"));
        Parcelle parcelle = parcelleRepository.findById(request.getIdParcelle())
                .orElseThrow(() -> new RuntimeException("Parcelle non trouvée"));

        NotificationOccupation notification = NotificationOccupation.builder()
                .idNotification(UUID.randomUUID())
                .dateNotification(request.getDateNotification())
                .annee(request.getAnnee())
                .dateConvocation(request.getDateConvocation())
                .informationsOccupants(request.getInformationsOccupants())
                .constats(request.getConstats())
                .doleances(request.getDoleances())
                .actionsEntreprises(request.getActionsEntreprises())
                .statut(request.getStatut())
                .titreFoncier(titreFoncier)
                .parcelle(parcelle)
                .build();
        notification = notificationRepository.save(notification);
        final NotificationOccupation savedNotification = notification;

        // Associer les personnes
        if (request.getPersonnes() != null && !request.getPersonnes().isEmpty()) {
            List<NotificationPersonne> personnes = request.getPersonnes().stream()
                    .map(p -> {
                        Personne personne = personneRepository.findById(p.getIdPersonne())
                                .orElseThrow(() -> new RuntimeException("Personne non trouvée avec l'id : " + p.getIdPersonne()));
                        return NotificationPersonne.builder()
                                .idNotification(savedNotification.getIdNotification())
                                .idPersonne(p.getIdPersonne())
                                .roleDansNotification(p.getRoleDansNotification())
                                .notificationOccupation(savedNotification)
                                .personne(personne)
                                .build();
                    })
                    .collect(Collectors.toList());
            notificationPersonneRepository.saveAll(personnes);
        }

        return toDTO(notification);
    }

    @Transactional
    public NotificationOccupationDTO mettreAJour(UUID id, NotificationOccupationRequest request) {
        NotificationOccupation notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée avec l'id : " + id));

        TitreFoncier titreFoncier = titreFoncierRepository.findById(request.getIdTitreFoncier())
                .orElseThrow(() -> new RuntimeException("Titre foncier non trouvé"));
        Parcelle parcelle = parcelleRepository.findById(request.getIdParcelle())
                .orElseThrow(() -> new RuntimeException("Parcelle non trouvée"));

        notification.setDateNotification(request.getDateNotification());
        notification.setAnnee(request.getAnnee());
        notification.setDateConvocation(request.getDateConvocation());
        notification.setInformationsOccupants(request.getInformationsOccupants());
        notification.setConstats(request.getConstats());
        notification.setDoleances(request.getDoleances());
        notification.setActionsEntreprises(request.getActionsEntreprises());
        notification.setStatut(request.getStatut());
        notification.setTitreFoncier(titreFoncier);
        notification.setParcelle(parcelle);

        // Mettre à jour les personnes
        notificationPersonneRepository.deleteByIdNotification(id);
        final NotificationOccupation updatedNotification = notification;
        if (request.getPersonnes() != null && !request.getPersonnes().isEmpty()) {
            List<NotificationPersonne> personnes = request.getPersonnes().stream()
                    .map(p -> {
                        Personne personne = personneRepository.findById(p.getIdPersonne())
                                .orElseThrow(() -> new RuntimeException("Personne non trouvée avec l'id : " + p.getIdPersonne()));
                        return NotificationPersonne.builder()
                                .idNotification(updatedNotification.getIdNotification())
                                .idPersonne(p.getIdPersonne())
                                .roleDansNotification(p.getRoleDansNotification())
                                .notificationOccupation(updatedNotification)
                                .personne(personne)
                                .build();
                    })
                    .collect(Collectors.toList());
            notificationPersonneRepository.saveAll(personnes);
        }

        return toDTO(notificationRepository.save(notification));
    }

    @Transactional
    public void supprimer(UUID id) {
        notificationPersonneRepository.deleteByIdNotification(id);
        notificationRepository.deleteById(id);
    }

    // ── Recherches par critère ──

    @Transactional(readOnly = true)
    public List<NotificationOccupationDTO> listerParStatut(String statut) {
        return notificationRepository.findByStatut(statut).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationOccupationDTO> listerParAnnee(Integer annee) {
        return notificationRepository.findByAnnee(annee).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationOccupationDTO> listerParTitreFoncier(UUID idTitreFoncier) {
        return notificationRepository.findByTitreFoncierIdTitreFoncier(idTitreFoncier).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationOccupationDTO> listerParParcelle(UUID idParcelle) {
        return notificationRepository.findByParcelleIdParcelle(idParcelle).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── Statistiques ──

    @Transactional(readOnly = true)
    public long compterParStatut(String statut) {
        return notificationRepository.countByStatut(statut);
    }

    @Transactional(readOnly = true)
    public long compterParAnnee(Integer annee) {
        return notificationRepository.countByAnnee(annee);
    }

    // ── Helpers ──

    private NotificationOccupationDTO toDTO(NotificationOccupation n) {
        List<NotificationPersonne> personnes = notificationPersonneRepository
                .findByIdNotification(n.getIdNotification());

        return NotificationOccupationDTO.builder()
                .idNotification(n.getIdNotification())
                .dateNotification(n.getDateNotification())
                .annee(n.getAnnee())
                .dateConvocation(n.getDateConvocation())
                .informationsOccupants(n.getInformationsOccupants())
                .constats(n.getConstats())
                .doleances(n.getDoleances())
                .actionsEntreprises(n.getActionsEntreprises())
                .statut(n.getStatut())
                .createdAt(n.getCreatedAt())
                .idParcelle(n.getParcelle() != null ? n.getParcelle().getIdParcelle() : null)
                .numeroLot(n.getParcelle() != null ? n.getParcelle().getNumeroLot() : null)
                .idTitreFoncier(n.getTitreFoncier() != null ? n.getTitreFoncier().getIdTitreFoncier() : null)
                .numeroTitre(n.getTitreFoncier() != null ? n.getTitreFoncier().getNumero() : null)
                .personnes(personnes.stream().map(this::toPersonneDTO).collect(Collectors.toList()))
                .build();
    }

    private NotificationPersonneDTO toPersonneDTO(NotificationPersonne np) {
        return NotificationPersonneDTO.builder()
                .idNotification(np.getIdNotification())
                .idPersonne(np.getIdPersonne())
                .nomPersonne(np.getPersonne() != null ? np.getPersonne().getNom() : null)
                .roleDansNotification(np.getRoleDansNotification())
                .contact(np.getPersonne() != null ? np.getPersonne().getContact() : null)
                .build();
    }
}
