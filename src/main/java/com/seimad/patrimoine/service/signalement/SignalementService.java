package com.seimad.patrimoine.service.signalement;

import com.seimad.patrimoine.dto.carte.GeometrieRequest;
import com.seimad.patrimoine.dto.signalement.AssignationDTO;
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
import com.seimad.patrimoine.entity.referentiel.Propriete;
import com.seimad.patrimoine.entity.referentiel.Ville;
import com.seimad.patrimoine.entity.signalement.Assignation;
import com.seimad.patrimoine.entity.signalement.Signalement;
import com.seimad.patrimoine.entity.signalement.StatutSignalement;
import com.seimad.patrimoine.entity.signalement.TypeSignalement;
import com.seimad.patrimoine.repository.auth.UtilisateurRepository;
import com.seimad.patrimoine.repository.carte.GeometrieRepository;
import com.seimad.patrimoine.repository.notification.AvertissementRepository;
import com.seimad.patrimoine.repository.notification.NotificationOccupationRepository;
import com.seimad.patrimoine.repository.referentiel.ParcelleRepository;
import com.seimad.patrimoine.repository.referentiel.ProprieteRepository;
import com.seimad.patrimoine.repository.referentiel.VilleRepository;
import com.seimad.patrimoine.repository.signalement.AssignationRepository;
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

import java.math.BigDecimal;
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
    private final GeometrieRepository geometrieRepository;
    private final AssignationRepository assignationRepository;
    private final TypeSignalementRepository typeSignalementRepository;
    private final StatutSignalementRepository statutSignalementRepository;
    private final VilleRepository villeRepository;
    private final ProprieteRepository proprieteRepository;
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
                .propriete(resoudrePropriete(request.getIdPropriete()))
                .parcelle(resoudreParcelle(request.getIdParcelle()))
                .dossier(resoudreDossier(request.getIdDossier()))
                .utilisateurCreation(resoudreUtilisateur(request.getIdUtilisateurCreation()))
                .build();
        if (request.getIdStatutSignalement() != null) {
            signalement.setStatutSignalement(statutSignalementRepository.findById(request.getIdStatutSignalement())
                    .orElseThrow(() -> new RuntimeException("Statut de signalement non trouvé")));
        }
        signalement.setDateModification(LocalDateTime.now());

        Signalement saved = signalementRepository.save(signalement);

        // Position GPS du constat → table `geometrie` (PostGIS), entite_type='signalement'.
        if (request.getGeometrie() != null) {
            enregistrerGeometrie(request.getGeometrie(), saved.getIdSignalement(),
                    saved.getUtilisateurCreation());
        }

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
        signalement.setPropriete(resoudrePropriete(request.getIdPropriete()));
        signalement.setParcelle(resoudreParcelle(request.getIdParcelle()));
        signalement.setDossier(resoudreDossier(request.getIdDossier()));
        if (request.getIdUtilisateurCreation() != null) {
            signalement.setUtilisateurCreation(resoudreUtilisateur(request.getIdUtilisateurCreation()));
        }
        signalement.setDateModification(LocalDateTime.now());

        Signalement saved = signalementRepository.save(signalement);

        // Géométrie fournie à la mise à jour (ex. repositionnement) → upsert PostGIS.
        if (request.getGeometrie() != null) {
            enregistrerGeometrie(request.getGeometrie(), id, saved.getUtilisateurCreation());
        }

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
        geometrieRepository.supprimerParEntite("signalement", id);
    }

    // ── Assignations (signalement <-> notification / avertissement) ──

    @Transactional(readOnly = true)
    public List<AssignationDTO> listerAssignations(UUID idSignalement) {
        Signalement signalement = trouverEntite(idSignalement);
        return assignationRepository
                .findBySignalementIdSignalementOrderByDateAssignationDesc(signalement.getIdSignalement())
                .stream().map(this::toAssignationDTO).collect(Collectors.toList());
    }

    @Transactional
    public AssignationDTO assignerNotification(UUID idSignalement, UUID idNotification) {
        Signalement signalement = trouverEntite(idSignalement);
        NotificationOccupation notification = notificationOccupationRepository.findById(idNotification)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée avec l'id : " + idNotification));
        // Idempotent : une assignation existante est renvoyée telle quelle
        var existante = assignationRepository
                .findBySignalementIdSignalementAndNotificationIdNotification(idSignalement, idNotification);
        if (existante.isPresent()) {
            return toAssignationDTO(existante.get());
        }
        Assignation assignation = Assignation.builder()
                .signalement(signalement)
                .notification(notification)
                .utilisateur(resoudreUtilisateur(null))
                .build();
        Assignation saved = assignationRepository.save(assignation);
        auditService.enregistrer("assignation", saved.getIdAssignation().toString(), "CREATE",
                null, valeursAuditAssignation(saved));
        return toAssignationDTO(saved);
    }

    @Transactional
    public AssignationDTO assignerAvertissement(UUID idSignalement, UUID idAvertissement) {
        Signalement signalement = trouverEntite(idSignalement);
        Avertissement avertissement = avertissementRepository.findById(idAvertissement)
                .orElseThrow(() -> new RuntimeException("Avertissement non trouvé avec l'id : " + idAvertissement));
        // Idempotent : une assignation existante est renvoyée telle quelle
        var existante = assignationRepository
                .findBySignalementIdSignalementAndAvertissementIdAvertissement(idSignalement, idAvertissement);
        if (existante.isPresent()) {
            return toAssignationDTO(existante.get());
        }
        Assignation assignation = Assignation.builder()
                .signalement(signalement)
                .avertissement(avertissement)
                .utilisateur(resoudreUtilisateur(null))
                .build();
        Assignation saved = assignationRepository.save(assignation);
        auditService.enregistrer("assignation", saved.getIdAssignation().toString(), "CREATE",
                null, valeursAuditAssignation(saved));
        return toAssignationDTO(saved);
    }

    @Transactional
    public void desassignerNotification(UUID idSignalement, UUID idNotification) {
        Assignation assignation = assignationRepository
                .findBySignalementIdSignalementAndNotificationIdNotification(idSignalement, idNotification)
                .orElseThrow(() -> new RuntimeException("Aucune assignation entre ce signalement et cette notification"));
        Map<String, Object> anciennesValeurs = valeursAuditAssignation(assignation);
        auditService.enregistrer("assignation", assignation.getIdAssignation().toString(), "DELETE",
                anciennesValeurs, null);
        assignationRepository.delete(assignation);
    }

    @Transactional
    public void desassignerAvertissement(UUID idSignalement, UUID idAvertissement) {
        Assignation assignation = assignationRepository
                .findBySignalementIdSignalementAndAvertissementIdAvertissement(idSignalement, idAvertissement)
                .orElseThrow(() -> new RuntimeException("Aucune assignation entre ce signalement et cet avertissement"));
        Map<String, Object> anciennesValeurs = valeursAuditAssignation(assignation);
        auditService.enregistrer("assignation", assignation.getIdAssignation().toString(), "DELETE",
                anciennesValeurs, null);
        assignationRepository.delete(assignation);
    }

    @Transactional(readOnly = true)
    public List<AssignationDTO> listerAssignationsParNotification(UUID idNotification) {
        return assignationRepository.findByNotificationIdNotification(idNotification).stream()
                .map(this::toAssignationDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignationDTO> listerAssignationsParAvertissement(UUID idAvertissement) {
        return assignationRepository.findByAvertissementIdAvertissement(idAvertissement).stream()
                .map(this::toAssignationDTO).collect(Collectors.toList());
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
        public List<SignalementDTO> listerParPropriete(Integer idPropriete) {
        return signalementRepository.findByProprieteIdPropriete(idPropriete).stream()
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

    private Propriete resoudrePropriete(Integer id) {
        return id != null ? proprieteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propriété non trouvée avec l'id : " + id)) : null;
    }

    private Parcelle resoudreParcelle(UUID id) {
        return id != null ? parcelleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parcelle non trouvée avec l'id : " + id)) : null;
    }

    /** Pas de DossierRepository dans le backend : référence via l'EntityManager. */
    private Dossier resoudreDossier(Integer idDossier) {
        return idDossier != null ? entityManager.getReference(Dossier.class, idDossier) : null;
    }

    private Signalement trouverEntite(UUID id) {
        return signalementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Signalement non trouvé avec l'id : " + id));
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

    /**
     * Persiste la géométrie du signalement (reçue en GeoJSON texte) dans la
     * table `geometrie` (PostGIS). Ignorée si absente ou si l'utilisateur
     * n'est pas résolu (colonne id_utilisateur NOT NULL côté base).
     */
    private void enregistrerGeometrie(GeometrieRequest geometrie, UUID idSignalement, Utilisateur utilisateur) {
        if (geometrie == null || geometrie.getGeojson() == null || geometrie.getGeojson().isBlank()) {
            return;
        }
        if (utilisateur == null) {
            log.warn("Géométrie ignorée pour le signalement {} : utilisateur non résolu", idSignalement);
            return;
        }
        String type = geometrie.getTypeGeometrie() != null ? geometrie.getTypeGeometrie() : "Point";
        String source = geometrie.getSource() != null && !geometrie.getSource().isBlank()
                ? geometrie.getSource() : "Point carte";
        BigDecimal precisionM = geometrie.getPrecisionM() != null
                ? BigDecimal.valueOf(geometrie.getPrecisionM()) : null;
        geometrieRepository.upsert("signalement", idSignalement, type, geometrie.getGeojson(),
                precisionM, source, utilisateur.getIdUtilisateur());
        log.info("Géométrie {} enregistrée pour le signalement {}", type, idSignalement);
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
        valeurs.put("numeroPropriete", s.getPropriete() != null ? s.getPropriete().getNumero() : null);
        valeurs.put("numeroLot", s.getParcelle() != null ? s.getParcelle().getNumeroLot() : null);
        valeurs.put("commentaireTraitement", s.getCommentaireTraitement());
        valeurs.put("dateTraitement", s.getDateTraitement());
        valeurs.put("utilisateurTraitement",
                s.getUtilisateurTraitement() != null ? s.getUtilisateurTraitement().getNomComplet() : null);
        return valeurs;
    }

    /** Valeurs lisibles pour l'audit d'une assignation. */
    private Map<String, Object> valeursAuditAssignation(Assignation a) {
        Map<String, Object> valeurs = new LinkedHashMap<>();
        valeurs.put("signalement", a.getSignalement() != null ? a.getSignalement().getReference() : null);
        valeurs.put("typeCible", a.getNotification() != null ? "notification"
                : a.getAvertissement() != null ? "avertissement" : null);
        valeurs.put("notification", a.getNotification() != null ? a.getNotification().getIdNotification().toString() : null);
        valeurs.put("avertissement", a.getAvertissement() != null ? a.getAvertissement().getIdAvertissement().toString() : null);
        valeurs.put("dateAssignation", a.getDateAssignation());
        return valeurs;
    }

    private AssignationDTO toAssignationDTO(Assignation a) {
        AssignationDTO.AssignationDTOBuilder b = AssignationDTO.builder()
                .idAssignation(a.getIdAssignation())
                .idSignalement(a.getSignalement() != null ? a.getSignalement().getIdSignalement() : null)
                .referenceSignalement(a.getSignalement() != null ? a.getSignalement().getReference() : null)
                .dateAssignation(a.getDateAssignation())
                .idUtilisateur(a.getUtilisateur() != null ? a.getUtilisateur().getIdUtilisateur() : null)
                .nomUtilisateur(a.getUtilisateur() != null ? a.getUtilisateur().getNomComplet() : null);
        if (a.getNotification() != null) {
            b.typeCible("notification")
                    .idNotification(a.getNotification().getIdNotification())
                    .dateNotification(a.getNotification().getDateNotification())
                    .numeroProprieteNotification(a.getNotification().getPropriete() != null
                            ? a.getNotification().getPropriete().getNumero() : null);
        }
        if (a.getAvertissement() != null) {
            b.typeCible("avertissement")
                    .idAvertissement(a.getAvertissement().getIdAvertissement())
                    .dateAvertissement(a.getAvertissement().getDateAvertissement())
                    .numeroProprieteAvertissement(a.getAvertissement().getPropriete() != null
                            ? a.getAvertissement().getPropriete().getNumero() : null);
        }
        return b.build();
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
                .idPropriete(s.getPropriete() != null ? s.getPropriete().getIdPropriete() : null)
                .numeroPropriete(s.getPropriete() != null ? s.getPropriete().getNumero() : null)
                .idParcelle(s.getParcelle() != null ? s.getParcelle().getIdParcelle() : null)
                .numeroLot(s.getParcelle() != null ? s.getParcelle().getNumeroLot() : null)
                .commentaireTraitement(s.getCommentaireTraitement())
                .dateTraitement(s.getDateTraitement())
                .idUtilisateurTraitement(s.getUtilisateurTraitement() != null ? s.getUtilisateurTraitement().getIdUtilisateur() : null)
                .nomUtilisateurTraitement(s.getUtilisateurTraitement() != null ? s.getUtilisateurTraitement().getNomComplet() : null)
                .idDossier(s.getDossier() != null ? s.getDossier().getIdDossier() : null)
                .numeroDossier(s.getDossier() != null ? s.getDossier().getNumeroDossier() : null)
                .assignations(assignationRepository
                        .findBySignalementIdSignalementOrderByDateAssignationDesc(s.getIdSignalement())
                        .stream().map(this::toAssignationDTO).collect(Collectors.toList()))
                .idUtilisateurCreation(s.getUtilisateurCreation() != null ? s.getUtilisateurCreation().getIdUtilisateur() : null)
                .nomUtilisateurCreation(s.getUtilisateurCreation() != null ? s.getUtilisateurCreation().getNomComplet() : null)
                .build();
    }
}