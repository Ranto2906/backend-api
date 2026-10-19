package com.seimad.patrimoine.service.notification;

import com.seimad.patrimoine.dto.notification.EtapeNotificationDTO;
import com.seimad.patrimoine.dto.notification.EtapeNotificationRequest;
import com.seimad.patrimoine.dto.notification.SuiviNotificationDTO;
import com.seimad.patrimoine.dto.notification.SuiviNotificationRequest;
import com.seimad.patrimoine.entity.dossier.StatutCouleur;
import com.seimad.patrimoine.entity.notification.EtapeNotification;
import com.seimad.patrimoine.entity.notification.NotificationOccupation;
import com.seimad.patrimoine.entity.notification.SuiviNotification;
import com.seimad.patrimoine.repository.dossier.StatutCouleurRepository;
import com.seimad.patrimoine.repository.notification.EtapeNotificationRepository;
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
    private final EtapeNotificationRepository etapeNotificationRepository;
    private final StatutCouleurRepository statutCouleurRepository;
    private final AuditService auditService;

    // ── Référentiel des étapes (workflow type dossier) ──

    @Transactional(readOnly = true)
    public List<EtapeNotificationDTO> listerEtapes() {
        return etapeNotificationRepository.findAllByOrderByOrdreAsc().stream()
                .map(this::toEtapeDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EtapeNotificationDTO trouverEtape(Integer idEtape) {
        EtapeNotification etape = etapeNotificationRepository.findById(idEtape)
                .orElseThrow(() -> new RuntimeException("Étape non trouvée avec l'id : " + idEtape));
        return toEtapeDTO(etape);
    }

    @Transactional
    public EtapeNotificationDTO creerEtape(EtapeNotificationRequest request) {
        String code = normaliserCode(request.getCodeEtape());
        etapeNotificationRepository.findByCodeEtape(code).ifPresent(e -> {
            throw new RuntimeException("Une étape avec le code " + code + " existe déjà");
        });
        etapeNotificationRepository.findByOrdre(request.getOrdre()).ifPresent(e -> {
            throw new RuntimeException("Une étape d'ordre " + request.getOrdre() + " existe déjà");
        });

        EtapeNotification etape = EtapeNotification.builder()
                .codeEtape(code)
                .libelle(request.getLibelle().trim())
                .ordre(request.getOrdre())
                .estFinal(Boolean.TRUE.equals(request.getEstFinal()))
                .build();
        EtapeNotification saved = etapeNotificationRepository.save(etape);

        auditService.enregistrer("etape_notification", saved.getIdEtapeNotification().toString(),
                "CREATE", null, valeursAuditEtape(saved));
        return toEtapeDTO(saved);
    }

    @Transactional
    public EtapeNotificationDTO modifierEtape(Integer idEtape, EtapeNotificationRequest request) {
        EtapeNotification etape = etapeNotificationRepository.findById(idEtape)
                .orElseThrow(() -> new RuntimeException("Étape non trouvée avec l'id : " + idEtape));

        String code = normaliserCode(request.getCodeEtape());
        etapeNotificationRepository.findByCodeEtape(code)
                .filter(e -> !e.getIdEtapeNotification().equals(idEtape))
                .ifPresent(e -> {
                    throw new RuntimeException("Une étape avec le code " + code + " existe déjà");
                });
        etapeNotificationRepository.findByOrdre(request.getOrdre())
                .filter(e -> !e.getIdEtapeNotification().equals(idEtape))
                .ifPresent(e -> {
                    throw new RuntimeException("Une étape d'ordre " + request.getOrdre() + " existe déjà");
                });

        Map<String, Object> anciennes = valeursAuditEtape(etape);

        etape.setCodeEtape(code);
        etape.setLibelle(request.getLibelle().trim());
        etape.setOrdre(request.getOrdre());
        etape.setEstFinal(Boolean.TRUE.equals(request.getEstFinal()));
        EtapeNotification saved = etapeNotificationRepository.save(etape);

        auditService.enregistrer("etape_notification", idEtape.toString(),
                "UPDATE", anciennes, valeursAuditEtape(saved));
        return toEtapeDTO(saved);
    }

    /**
     * Supprime une étape du référentiel. Refusé si l'étape est utilisée
     * dans le suivi d'une ou plusieurs notifications (traçabilité).
     */
    @Transactional
    public void supprimerEtape(Integer idEtape) {
        EtapeNotification etape = etapeNotificationRepository.findById(idEtape)
                .orElseThrow(() -> new RuntimeException("Étape non trouvée avec l'id : " + idEtape));
        if (suiviRepository.existsByEtapeNotificationIdEtapeNotification(idEtape)) {
            throw new RuntimeException("Impossible de supprimer l'étape « " + etape.getLibelle()
                    + " » : elle est utilisée dans le suivi d'une ou plusieurs notifications.");
        }
        etapeNotificationRepository.delete(etape);
        auditService.enregistrer("etape_notification", idEtape.toString(),
                "DELETE", valeursAuditEtape(etape), null);
    }

    /** Code majuscule à underscores. */
    private String normaliserCode(String code) {
        String normalise = code == null ? "" : code.trim().toUpperCase().replaceAll("\\s+", "_");
        if (normalise.length() > 50) normalise = normalise.substring(0, 50);
        return normalise;
    }

    private Map<String, Object> valeursAuditEtape(EtapeNotification e) {
        Map<String, Object> valeurs = new LinkedHashMap<>();
        valeurs.put("code", e.getCodeEtape());
        valeurs.put("libelle", e.getLibelle());
        valeurs.put("ordre", e.getOrdre());
        valeurs.put("estFinal", e.getEstFinal());
        return valeurs;
    }

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
                .etapeNotification(resoudreEtape(request.getIdEtapeNotification()))
                .statutCouleur(resoudreCouleur(request.getIdStatutCouleur()))
                .commentaire(request.getCommentaire())
                .notificationOccupation(notification)
                .build();
        SuiviNotification saved = suiviRepository.save(suivi);

        // Étape finale : la notification passe automatiquement en « Clôturée »
        if (saved.getEtapeNotification() != null && Boolean.TRUE.equals(saved.getEtapeNotification().getEstFinal())) {
            notification.setStatut("Clôturée");
            notificationRepository.save(notification);
        }

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
        suivi.setEtapeNotification(resoudreEtape(request.getIdEtapeNotification()));
        suivi.setStatutCouleur(resoudreCouleur(request.getIdStatutCouleur()));
        suivi.setCommentaire(request.getCommentaire());

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
        valeurs.put("libelleEtape",
                s.getEtapeNotification() != null ? s.getEtapeNotification().getLibelle() : null);
        valeurs.put("statutCouleur",
                s.getStatutCouleur() != null ? s.getStatutCouleur().getLibelle() : null);
        valeurs.put("commentaire", s.getCommentaire());

        NotificationOccupation n = s.getNotificationOccupation();
        if (n != null) {
            valeurs.put("numeroPropriete", n.getPropriete() != null ? n.getPropriete().getNumero() : null);
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
                .actionsASuivre(s.getActionsASuivre())
                .commentaire(s.getCommentaire());

        if (s.getEtapeNotification() != null) {
            EtapeNotification e = s.getEtapeNotification();
            b.idEtapeNotification(e.getIdEtapeNotification())
             .codeEtape(e.getCodeEtape())
             .libelleEtape(e.getLibelle())
             .ordreEtape(e.getOrdre())
             .etapeFinale(e.getEstFinal());
        }
        if (s.getStatutCouleur() != null) {
            StatutCouleur c = s.getStatutCouleur();
            b.idStatutCouleur(c.getIdStatutCouleur())
             .codeStatutCouleur(c.getCode())
             .libelleStatutCouleur(c.getLibelle())
             .couleurStatutCouleurHex(c.getCouleurHex());
        }

        NotificationOccupation n = s.getNotificationOccupation();
        if (n != null) {
            b.idNotification(n.getIdNotification())
             .numeroPropriete(n.getPropriete() != null ? n.getPropriete().getNumero() : null)
             .numeroLot(n.getParcelle() != null ? n.getParcelle().getNumeroLot() : null)
             .dateNotification(n.getDateNotification())
             .statut(n.getStatut());
        }
        return b.build();
    }

    private EtapeNotificationDTO toEtapeDTO(EtapeNotification e) {
        return EtapeNotificationDTO.builder()
                .idEtapeNotification(e.getIdEtapeNotification())
                .codeEtape(e.getCodeEtape())
                .libelle(e.getLibelle())
                .ordre(e.getOrdre())
                .estFinal(e.getEstFinal())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private EtapeNotification resoudreEtape(Integer idEtape) {
        if (idEtape == null) return null;
        return etapeNotificationRepository.findById(idEtape)
                .orElseThrow(() -> new RuntimeException("Étape non trouvée avec l'id : " + idEtape));
    }

    private StatutCouleur resoudreCouleur(Integer idCouleur) {
        if (idCouleur == null) return null;
        return statutCouleurRepository.findById(idCouleur)
                .orElseThrow(() -> new RuntimeException("Couleur d'avancement non trouvée avec l'id : " + idCouleur));
    }
}
