package com.seimad.patrimoine.service.notification;

import com.seimad.patrimoine.dto.notification.SuiviNotificationDTO;
import com.seimad.patrimoine.dto.notification.SuiviNotificationRequest;
import com.seimad.patrimoine.entity.notification.NotificationOccupation;
import com.seimad.patrimoine.entity.notification.SuiviNotification;
import com.seimad.patrimoine.repository.notification.NotificationOccupationRepository;
import com.seimad.patrimoine.repository.notification.SuiviNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SuiviNotificationService {

    private final SuiviNotificationRepository suiviRepository;
    private final NotificationOccupationRepository notificationRepository;

    // ── CRUD ──

    @Transactional(readOnly = true)
    public List<SuiviNotificationDTO> listerParNotification(UUID idNotification) {
        return suiviRepository.findByNotificationOccupationIdNotificationOrderByOrdreAsc(idNotification)
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
    public SuiviNotificationDTO creer(UUID idNotification, SuiviNotificationRequest request) {
        NotificationOccupation notification = notificationRepository.findById(idNotification)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée avec l'id : " + idNotification));

        // Déterminer le prochain ordre
        long nombreSuivis = suiviRepository.countByNotificationOccupationIdNotification(idNotification);
        Integer ordre = request.getOrdre() != null ? request.getOrdre() : (int) (nombreSuivis + 1);

        SuiviNotification suivi = SuiviNotification.builder()
                .idSuivi(UUID.randomUUID())
                .ordre(ordre)
                .dateSuivi(request.getDateSuivi())
                .constats(request.getConstats())
                .actionsASuivre(request.getActionsASuivre())
                .notificationOccupation(notification)
                .build();
        return toDTO(suiviRepository.save(suivi));
    }

    @Transactional
    public SuiviNotificationDTO mettreAJour(UUID id, SuiviNotificationRequest request) {
        SuiviNotification suivi = suiviRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Suivi non trouvé avec l'id : " + id));

        suivi.setOrdre(request.getOrdre() != null ? request.getOrdre() : suivi.getOrdre());
        suivi.setDateSuivi(request.getDateSuivi());
        suivi.setConstats(request.getConstats());
        suivi.setActionsASuivre(request.getActionsASuivre());

        return toDTO(suiviRepository.save(suivi));
    }

    @Transactional
    public void supprimer(UUID id) {
        suiviRepository.deleteById(id);
    }

    // ── Helpers ──

    private SuiviNotificationDTO toDTO(SuiviNotification s) {
        return SuiviNotificationDTO.builder()
                .idSuivi(s.getIdSuivi())
                .ordre(s.getOrdre())
                .dateSuivi(s.getDateSuivi())
                .constats(s.getConstats())
                .actionsASuivre(s.getActionsASuivre())
                .idNotification(s.getNotificationOccupation().getIdNotification())
                .build();
    }
}
