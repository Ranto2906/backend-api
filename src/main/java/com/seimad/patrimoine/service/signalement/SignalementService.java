package com.seimad.patrimoine.service.signalement;

import com.seimad.patrimoine.dto.signalement.SignalementDTO;
import com.seimad.patrimoine.dto.signalement.SignalementRequest;
import com.seimad.patrimoine.dto.signalement.SignalementTraitementRequest;
import com.seimad.patrimoine.dto.signalement.StatutSignalementDTO;
import com.seimad.patrimoine.dto.signalement.TypeSignalementDTO;
import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.dossier.Dossier;
import com.seimad.patrimoine.entity.notification.Avertissement;
import com.seimad.patrimoine.entity.notification.NotificationOccupation;
import com.seimad.patrimoine.entity.referentiel.Parcelle;
import com.seimad.patrimoine.entity.referentiel.TitreFoncier;
import com.seimad.patrimoine.entity.referentiel.Ville;
import com.seimad.patrimoine.entity.signalement.Signalement;
import com.seimad.patrimoine.entity.signalement.StatutSignalement;
import com.seimad.patrimoine.entity.signalement.TypeSignalement;
import com.seimad.patrimoine.repository.auth.UtilisateurRepository;
import com.seimad.patrimoine.repository.notification.AvertissementRepository;
import com.seimad.patrimoine.repository.notification.NotificationOccupationRepository;
import com.seimad.patrimoine.repository.referentiel.ParcelleRepository;
import com.seimad.patrimoine.repository.referentiel.TitreFoncierRepository;
import com.seimad.patrimoine.repository.referentiel.VilleRepository;
import com.seimad.patrimoine.repository.signalement.SignalementRepository;
import com.seimad.patrimoine.repository.signalement.StatutSignalementRepository;
import com.seimad.patrimoine.repository.signalement.TypeSignalementRepository;
import com.seimad.patrimoine.service.dossier.AuditService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SignalementService {

    private final SignalementRepository signalementRepository;
    private final TypeSignalementRepository typeSignalementRepository;
    private final StatutSignalementRepository statutSignalementRepository;
    private final VilleRepository villeRepository;
    private final TitreFoncierRepository titreFoncierRepository;
    private final ParcelleRepository parcelleRepository;
    private final NotificationOccupationRepository notificationOccupationRepository;
    private final AvertissementRepository avertissementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;

    @PersistenceContext
    private EntityManager entityManager;

    // ── Référentiels ──

    @Transactional(readOnly = true)
    public List<TypeSignalementDTO> listerTypes() {
        return typeSignalementRepository.findAll().stream()
                .map(t -> TypeSignalementDTO.builder()
                        .idTypeSignalement(t.getIdTypeSignalement())
                        .code(t.getCode())
                        .libelle(t.getLibelle())
                        .couleur(t.getCouleur())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StatutSignalementDTO> listerStatuts() {
        return statutSignalementRepository.findAll().stream()
                .map(s -> StatutSignalementDTO.builder()
                        .idStatutSignalement(s.getIdStatutSignalement())
                        .code(s.getCode())
                        .libelle(s.getLibelle())
                        .couleurHex(s.getCouleurHex())
                        .estFinal(s.getEstFinal())
                        .ordre(s.getOrdre())
                        .build())
                .collect(Collectors.toList());
    }

    // ── CRUD ──

    @Transactional(readOnly = true)
    public Page<SignalementDTO> lister(Pageable pageable) {
        return signalementRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<SignalementDTO> rechercher(String search, Pageable pageable) {
        return signalementRepository.search(search, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public SignalementDTO trouverParId(UUID id) {
        Signalement signalement = signalementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Signalement non trouvé avec l'id : " + id));
        return toDTO(signalement);
    }

    @Transactional
    public SignalementDTO creer(SignalementRequest request) {
        TypeSignalement type = typeSignalementRepository.findById(request.getIdTypeSignalement())
                .orElseThrow(() -> new RuntimeException("Type de signalement non trouvé avec l'id : " + request.getIdTypeSignalement()));

        Signalement signalement = Signalement.builder()
                .idSignalement(UUID.randomUUID())
                .reference(resoudreReference(request))
                .description(request.getDescription())
                .dateSignalement(request.getDateSignalement() != null ? request.getDateSignalement() : LocalDateTime.now())
                .typeSignalement(type)
                .ville(resoudreVille(request.getIdVille()))
                .titreFoncier(resoudreTitreFoncier(request.getIdTitreFoncier()))
                .parcelle(resoudreParcelle(request.getIdParcelle()))
                .dossier(resoudreDossier(request.getIdDossier()))
                .notificationOccupation(resoudreNotification(request.getIdNotification()))
                .avertissement(resoudreAvertissement(request.getIdAvertissement()))
                .utilisateurCreation(resoudreUtilisateur(request.getIdUtilisateurCreation()))
                .build();
        if (request.getIdStatutSignalement() != null) {
            signalement.setStatutSignalement(statutSignalementRepository.findById(request.getIdStatutSignalement())
                    .orElseThrow(() -> new RuntimeException("Statut de signalement non trouvé")));
        }
        signalement.setDateModification(LocalDateTime.now());

        Signalement saved = signalementRepository.save(signalement);

        auditService.enregistrer("signalement", saved.getIdSignalement().toString(), "CREATE",
                null, valeursAudit(saved));
        return toDTO(saved);
    }

    @Transactional
    public SignalementDTO mettreAJour(UUID id, SignalementRequest request) {
        Signalement signalement = signalementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Signalement non trouvé avec l'id : " + id));

        Map<String, Object> anciennesValeurs = valeursAudit(signalement);

        TypeSignalement type = typeSignalementRepository.findById(request.getIdTypeSignalement())
                .orElseThrow(() -> new RuntimeException("Type de signalement non trouvé avec l'id : " + request.getIdTypeSignalement()));

        if (request.getReference() != null && !request.getReference().isBlank()) {
            signalement.setReference(request.getReference());
        }
        signalement.setDescription(request.getDescription());
        if (request.getDateSignalement() != null) {
            signalement.setDateSignalement(request.getDateSignalement());
        }
        signalement.setTypeSignalement(type);
        if (request.getIdStatutSignalement() != null) {
            signalement.setStatutSignalement(statutSignalementRepository.findById(request.getIdStatutSignalement())
                    .orElseThrow(() -> new RuntimeException("Statut de signalement non trouvé")));
        }
        signalement.setVille(resoudreVille(request.getIdVille()));
        signalement.setTitreFoncier(resoudreTitreFoncier(request.getIdTitreFoncier()));
        signalement.setParcelle(resoudreParcelle(request.getIdParcelle()));
        signalement.setDossier(resoudreDossier(request.getIdDossier()));
        signalement.setNotificationOccupation(resoudreNotification(request.getIdNotification()));
        signalement.setAvertissement(resoudreAvertissement(request.getIdAvertissement()));
        if (request.getIdUtilisateurCreation() != null) {
            signalement.setUtilisateurCreation(resoudreUtilisateur(request.getIdUtilisateurCreation()));
        }
        signalement.setDateModification(LocalDateTime.now());

        Signalement saved = signalementRepository.save(signalement);

        auditService.enregistrer("signalement", id.toString(), "UPDATE",
                anciennesValeurs, valeursAudit(saved));
        return toDTO(saved);
    }

    /**
     * Traitement d'un signalement : changement de statut + commentaire + date de
     * traitement + utilisateur de traitement. Trace l'action en audit.
     */
    @Transactional
    public SignalementDTO traiter(UUID id, SignalementTraitementRequest request) {
        Signalement signalement = signalementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Signalement non trouvé avec l'id : " + id));

        Map<String, Object> anciennesValeurs = valeursAudit(signalement);

        StatutSignalement statut = statutSignalementRepository.findById(request.getIdStatutSignalement())
                .orElseThrow(() -> new RuntimeException("Statut de signalement non trouvé avec l'id : " + request.getIdStatutSignalement()));

        Utilisateur utilisateur = resoudreUtilisateur(request.getIdUtilisateurTraitement());

        signalement.setStatutSignalement(statut);
        signalement.setCommentaireTraitement(request.getCommentaireTraitement());
        signalement.setDateTraitement(LocalDateTime.now());
        signalement.setUtilisateurTraitement(utilisateur);
        signalement.setDateModification(LocalDateTime.now());

        Signalement saved = signalementRepository.save(signalement);

        auditService.enregistrer("signalement", id.toString(), "UPDATE",
                anciennesValeurs, valeursAudit(saved));
        return toDTO(saved);
    }

    @Transactional
    public void supprimer(UUID id) {
        Signalement signalement = signalementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Signalement non trouvé avec l'id : " + id));

        Map<String, Object> anciennesValeurs = valeursAudit(signalement);
        auditService.enregistrer("signalement", id.toString(), "DELETE", anciennesValeurs, null);
        signalementRepository.deleteById(id);
    }

    // ── Filtres ──

    @Transactional(readOnly = true)
    public List<SignalementDTO> listerParVille(Integer idVille) {
        return signalementRepository.findByVilleIdVille(idVille).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SignalementDTO> listerParStatut(Integer idStatutSignalement) {
        return signalementRepository.findByStatutSignalementIdStatutSignalement(idStatutSignalement).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SignalementDTO> listerParType(Integer idTypeSignalement) {
        return signalementRepository.findByTypeSignalementIdTypeSignalement(idTypeSignalement).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SignalementDTO> listerParTitreFoncier(UUID idTitreFoncier) {
        return signalementRepository.findByTitreFoncierIdTitreFoncier(idTitreFoncier).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SignalementDTO> listerParParcelle(UUID idParcelle) {
        return signalementRepository.findByParcelleIdParcelle(idParcelle).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    // ── Statistiques ──

    @Transactional(readOnly = true)
    public Map<String, Object> statistiques() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long total = signalementRepository.count();
        stats.put("total", total);
        Map<String, Long> parStatut = statutSignalementRepository.findAll().stream()
                .collect(Collectors.toMap(
                        s -> s.getLibelle(),
                        s -> signalementRepository.countByStatutSignalementIdStatutSignalement(s.getIdStatutSignalement()),
                        (a, b) -> a,
                        LinkedHashMap::new));
        stats.put("parStatut", parStatut);
        Map<String, Long> parType = typeSignalementRepository.findAll().stream()
                .collect(Collectors.toMap(
                        t -> t.getLibelle(),
                        t -> signalementRepository.countByTypeSignalementIdTypeSignalement(t.getIdTypeSignalement()),
                        (a, b) -> a,
                        LinkedHashMap::new));
        stats.put("parType", parType);
        return stats;
    }

    // ── Helpers ──

    /** Référence fournie ou générée : SIG-AAAA-NNNN (numéro max de l'année + 1). */
    private String resoudreReference(SignalementRequest request) {
        if (request.getReference() != null && !request.getReference().isBlank()) {
            return request.getReference();
        }
        int annee = LocalDate.now().getYear();
        String prefix = "SIG-" + annee + "-";
        Long max = signalementRepository.maxNumeroReference(prefix, prefix.length() + 1);
        return prefix + String.format("%04d", (max == null ? 0 : max) + 1);
    }

    private Ville resoudreVille(Integer idVille) {
        return idVille != null ? villeRepository.findById(idVille)
                .orElseThrow(() -> new RuntimeException("Ville non trouvée avec l'id : " + idVille)) : null;
    }

    private TitreFoncier resoudreTitreFoncier(UUID id) {
        return id != null ? titreFoncierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Titre foncier non trouvé avec l'id : " + id)) : null;
    }

    private Parcelle resoudreParcelle(UUID id) {
        return id != null ? parcelleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parcelle non trouvée avec l'id : " + id)) : null;
    }

    /** Pas de DossierRepository dans le backend : référence via l'EntityManager. */
    private Dossier resoudreDossier(Integer idDossier) {
        return idDossier != null ? entityManager.getReference(Dossier.class, idDossier) : null;
    }

    private NotificationOccupation resoudreNotification(UUID id) {
        return id != null ? notificationOccupationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée avec l'id : " + id)) : null;
    }

    private Avertissement resoudreAvertissement(UUID id) {
        return id != null ? avertissementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avertissement non trouvé avec l'id : " + id)) : null;
    }

    private Utilisateur resoudreUtilisateur(Integer idUtilisateur) {
        if (idUtilisateur != null) {
            return utilisateurRepository.findById(idUtilisateur)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id : " + idUtilisateur));
        }
        // Sinon, utilisateur connecté
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getPrincipal())) return null;
        return utilisateurRepository.findByNomUtilisateur(auth.getName()).orElse(null);
    }

    /** Valeurs métier lisibles pour l'audit d'un signalement. */
    private Map<String, Object> valeursAudit(Signalement s) {
        Map<String, Object> valeurs = new LinkedHashMap<>();
        valeurs.put("reference", s.getReference());
        valeurs.put("description", s.getDescription());
        valeurs.put("dateSignalement", s.getDateSignalement());
        valeurs.put("typeSignalement", s.getTypeSignalement() != null ? s.getTypeSignalement().getLibelle() : null);
        valeurs.put("statutSignalement", s.getStatutSignalement() != null ? s.getStatutSignalement().getLibelle() : null);
        valeurs.put("ville", s.getVille() != null ? s.getVille().getNomVille() : null);
        valeurs.put("numeroTitre", s.getTitreFoncier() != null ? s.getTitreFoncier().getNumero() : null);
        valeurs.put("numeroLot", s.getParcelle() != null ? s.getParcelle().getNumeroLot() : null);
        valeurs.put("commentaireTraitement", s.getCommentaireTraitement());
        valeurs.put("dateTraitement", s.getDateTraitement());
        valeurs.put("utilisateurTraitement",
                s.getUtilisateurTraitement() != null ? s.getUtilisateurTraitement().getNomComplet() : null);
        return valeurs;
    }

    private SignalementDTO toDTO(Signalement s) {
        return SignalementDTO.builder()
                .idSignalement(s.getIdSignalement())
                .reference(s.getReference())
                .description(s.getDescription())
                .dateSignalement(s.getDateSignalement())
                .dateModification(s.getDateModification())
                .idTypeSignalement(s.getTypeSignalement() != null ? s.getTypeSignalement().getIdTypeSignalement() : null)
                .codeType(s.getTypeSignalement() != null ? s.getTypeSignalement().getCode() : null)
                .libelleType(s.getTypeSignalement() != null ? s.getTypeSignalement().getLibelle() : null)
                .couleurType(s.getTypeSignalement() != null ? s.getTypeSignalement().getCouleur() : null)
                .idStatutSignalement(s.getStatutSignalement() != null ? s.getStatutSignalement().getIdStatutSignalement() : null)
                .codeStatut(s.getStatutSignalement() != null ? s.getStatutSignalement().getCode() : null)
                .libelleStatut(s.getStatutSignalement() != null ? s.getStatutSignalement().getLibelle() : null)
                .couleurStatutHex(s.getStatutSignalement() != null ? s.getStatutSignalement().getCouleurHex() : null)
                .statutFinal(s.getStatutSignalement() != null ? s.getStatutSignalement().getEstFinal() : null)
                .idVille(s.getVille() != null ? s.getVille().getIdVille() : null)
                .nomVille(s.getVille() != null ? s.getVille().getNomVille() : null)
                .idTitreFoncier(s.getTitreFoncier() != null ? s.getTitreFoncier().getIdTitreFoncier() : null)
                .numeroTitre(s.getTitreFoncier() != null ? s.getTitreFoncier().getNumero() : null)
                .idParcelle(s.getParcelle() != null ? s.getParcelle().getIdParcelle() : null)
                .numeroLot(s.getParcelle() != null ? s.getParcelle().getNumeroLot() : null)
                .commentaireTraitement(s.getCommentaireTraitement())
                .dateTraitement(s.getDateTraitement())
                .idUtilisateurTraitement(s.getUtilisateurTraitement() != null ? s.getUtilisateurTraitement().getIdUtilisateur() : null)
                .nomUtilisateurTraitement(s.getUtilisateurTraitement() != null ? s.getUtilisateurTraitement().getNomComplet() : null)
                .idDossier(s.getDossier() != null ? s.getDossier().getIdDossier() : null)
                .numeroDossier(s.getDossier() != null ? s.getDossier().getNumeroDossier() : null)
                .idNotification(s.getNotificationOccupation() != null ? s.getNotificationOccupation().getIdNotification() : null)
                .idAvertissement(s.getAvertissement() != null ? s.getAvertissement().getIdAvertissement() : null)
                .idUtilisateurCreation(s.getUtilisateurCreation() != null ? s.getUtilisateurCreation().getIdUtilisateur() : null)
                .nomUtilisateurCreation(s.getUtilisateurCreation() != null ? s.getUtilisateurCreation().getNomComplet() : null)
                .build();
    }
}