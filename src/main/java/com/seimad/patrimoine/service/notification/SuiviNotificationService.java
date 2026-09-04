package com.seimad.patrimoine.service.notification;

import com.seimad.patrimoine.dto.notification.SuiviNotificationDTO;
import com.seimad.patrimoine.dto.notification.SuiviNotificationRequest;
import com.seimad.patrimoine.entity.notification.NotificationOccupation;
import com.seimad.patrimoine.entity.notification.SuiviNotification;
import com.seimad.patrimoine.repository.notification.NotificationOccupationRepository;
import com.seimad.patrimoine.repository.notification.SuiviNotificationRepository;
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
public class SuiviNotificationService {

    private final SuiviNotificationRepository suiviRepository;
    private final NotificationOccupationRepository notificationRepository;
    private final AuditService auditService;

    // ── CRUD ──

    @Transactional(readOnly = true)
    public Page<SuiviNotificationDTO> lister(Pageable pageable) {
        return suiviRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<SuiviNotificationDTO> rechercher(String search, Pageable pageable) {
        return suiviRepository.search(search, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<SuiviNotificationDTO> listerParNotification(UUID idNotification) {
        return suiviRepository
                .findByNotificationOccupationIdNotificationOrderByOrdreAsc(idNotification)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SuiviNotificationDTO trouverParId(UUID id) {
        SuiviNotification suivi = suiviRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Suivi non trouvé avec l'id : " + id));
        return toDTO(suivi);
    }

    @Transactional
    public SuiviNotificationDTO creer(SuiviNotificationRequest request) {
        if (request.getIdNotification() == null) {
            throw new RuntimeException("La notification est obligatoire pour créer un suivi");
        }
        NotificationOccupation notification = notificationRepository.findById(request.getIdNotification())
                .orElseThrow(() -> new RuntimeException(
                        "Notification non trouvée avec l'id : " + request.getIdNotification()));

        // Ordre : auto (dernier + 1) ou vérification d'unicité
        long nombreSuivis = suiviRepository.countByNotificationOccupationIdNotification(
                notification.getIdNotification());
        Integer ordre = request.getOrdre() != null ? request.getOrdre() : (int) (nombreSuivis + 1);
        if (suiviRepository
                .findByNotificationOccupationIdNotificationAndOrdre(
                        notification.getIdNotification(), ordre)
                .isPresent()) {
            throw new RuntimeException(
                    "Un suivi porte déjà le n° " + ordre + " pour cette notification");
        }

        SuiviNotification suivi = SuiviNotification.builder()
                .idSuivi(UUID.randomUUID())
                .ordre(ordre)
                .dateSuivi(request.getDateSuivi())
                .constats(request.getConstats())
                .actionsASuivre(request.getActionsASuivre())
                .notificationOccupation(notification)
                .build();
        SuiviNotification saved = suiviRepository.save(suivi);

        // ── Audit de création ──
        auditService.enregistrer(
                "suivi_notification",
                saved.getIdSuivi().toString(),
                "CREATE",
                null,
                valeursAudit(saved)
        );
        return toDTO(saved);
    }

    @Transactional
    public SuiviNotificationDTO mettreAJour(UUID id, SuiviNotificationRequest request) {
        SuiviNotification suivi = suiviRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Suivi non trouvé avec l'id : " + id));

        Map<String, Object> anciennesValeurs = valeursAudit(suivi);

        suivi.setOrdre(request.getOrdre() != null ? request.getOrdre() : suivi.getOrdre());
        suivi.setDateSuivi(request.getDateSuivi());
        suivi.setConstats(request.getConstats());
        suivi.setActionsASuivre(request.getActionsASuivre());

        SuiviNotification saved = suiviRepository.save(suivi);

        // ── Audit de modification ──
        auditService.enregistrer(
                "suivi_notification",
                id.toString(),
                "UPDATE",
                anciennesValeurs,
                valeursAudit(saved)
        );
        return toDTO(saved);
    }

    @Transactional
    public void supprimer(UUID id) {
        SuiviNotification suivi = suiviRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Suivi non trouvé avec l'id : " + id));

        // ── Audit de suppression ──
        auditService.enregistrer(
                "suivi_notification",
                id.toString(),
                "DELETE",
                valeursAudit(suivi),
                null
        );

        suiviRepository.deleteById(id);
    }

    // ── Helpers ──

    /** Carte des valeurs (audit) : champs métier + contexte notification lisible. */
    private Map<String, Object> valeursAudit(SuiviNotification s) {
        Map<String, Object> valeurs = new LinkedHashMap<>();
        valeurs.put("ordre", s.getOrdre());
        valeurs.put("dateSuivi", s.getDateSuivi());
        valeurs.put("constats", s.getConstats());
        valeurs.put("actionsASuivre", s.getActionsASuivre());

        NotificationOccupation n = s.getNotificationOccupation();
        if (n != null) {
            valeurs.put("numeroTitre", n.getTitreFoncier() != null ? n.getTitreFoncier().getNumero() : null);
            valeurs.put("numeroLot", n.getParcelle() != null ? n.getParcelle().getNumeroLot() : null);
            valeurs.put("dateNotification", n.getDateNotification());
            valeurs.put("statut", n.getStatut());
        }
        return valeurs;
    }

    private SuiviNotificationDTO toDTO(SuiviNotification s) {
        SuiviNotificationDTO.SuiviNotificationDTOBuilder b = SuiviNotificationDTO.builder()
                .idSuivi(s.getIdSuivi())
                .ordre(s.getOrdre())
                .dateSuivi(s.getDateSuivi())
                .constats(s.getConstats())
                .actionsASuivre(s.getActionsASuivre());

        NotificationOccupation n = s.getNotificationOccupation();
        if (n != null) {
            b.idNotification(n.getIdNotification())
             .numeroTitre(n.getTitreFoncier() != null ? n.getTitreFoncier().getNumero() : null)
             .numeroLot(n.getParcelle() != null ? n.getParcelle().getNumeroLot() : null)
             .dateNotification(n.getDateNotification())
             .statut(n.getStatut());
        }
        return b.build();
    }
}
