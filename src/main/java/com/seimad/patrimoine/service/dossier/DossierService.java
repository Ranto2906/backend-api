package com.seimad.patrimoine.service.dossier;

import com.seimad.patrimoine.dto.dossier.AuditDTO;
import com.seimad.patrimoine.dto.dossier.DossierDTO;
import com.seimad.patrimoine.dto.dossier.DossierRequest;
import com.seimad.patrimoine.dto.dossier.EtapeRequest;
import com.seimad.patrimoine.dto.dossier.SuiviDossierRequest;
import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.dossier.*;
import com.seimad.patrimoine.entity.notification.Personne;
import com.seimad.patrimoine.entity.notification.TypePersonne;
import com.seimad.patrimoine.entity.referentiel.Parcelle;
import com.seimad.patrimoine.entity.referentiel.PrixM2;
import com.seimad.patrimoine.entity.referentiel.Propriete;
import com.seimad.patrimoine.entity.referentiel.Ville;
import com.seimad.patrimoine.repository.auth.UtilisateurRepository;
import com.seimad.patrimoine.repository.dossier.*;
import com.seimad.patrimoine.repository.notification.PersonneRepository;
import com.seimad.patrimoine.repository.dossier.StatutCouleurRepository;
import com.seimad.patrimoine.repository.notification.TypePersonneRepository;
import com.seimad.patrimoine.repository.referentiel.VilleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DossierService {

    private final DossierRepository dossierRepository;
    private final SuiviDossierRepository suiviDossierRepository;
    private final DossierDemandeurRepository dossierDemandeurRepository;
    private final EtapeRepository etapeRepository;
    private final StatutDossierRepository statutDossierRepository;
        private final TypeDossierRepository typeDossierRepository;
        private final DossierFournieRepository dossierFournieRepository;
    private final PersonneRepository personneRepository;
    private final TypePersonneRepository typePersonneRepository;
    private final StatutCouleurRepository statutCouleurRepository;
    private final VilleRepository villeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;
    private final DossierParcelleRepository dossierParcelleRepository;
    private final com.seimad.patrimoine.repository.referentiel.ParcelleRepository parcelleRepository;
    private final com.seimad.patrimoine.repository.referentiel.PrixM2Repository prixM2Repository;
    private final com.seimad.patrimoine.repository.referentiel.ProprieteRepository proprieteRepository;
    private final com.seimad.patrimoine.repository.descente.DescenteTerrainRepository descenteTerrainRepository;

    // ════════════════════════════════════════════════════════════
    // Lecture
    // ════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<DossierDTO> lister(Pageable pageable) {
        return dossierRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<DossierDTO> rechercher(String search, Integer idStatut, Integer idVille,
                                       Integer annee, LocalDate dateDebut, LocalDate dateFin,
                                       Pageable pageable) {
        return dossierRepository.search(search, idStatut, idVille, annee, dateDebut, dateFin, pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public DossierDTO trouverParId(Integer id) {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier non trouvé avec l'id : " + id));
        return toDTO(dossier);
    }

    @Transactional(readOnly = true)
    public List<DossierDTO.SuiviDossierDTO> historiqueSuivi(Integer id) {
        return suiviDossierRepository.findByDossierIdDossierOrderByEtapeOrdreAsc(id)
                .stream().map(this::toSuiviDTO).toList();
    }

    /**
     * Historique des modifications du dossier : entrées d'audit (création,
     * mises à jour, changements de statut) et suivi d'étapes, triées de la
     * plus récente à la plus ancienne.
     */
    @Transactional(readOnly = true)
    public List<AuditDTO> historiqueModifications(Integer id) {
        // Vérifie l'existence du dossier (404 clair si absent)
        dossierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier non trouvé avec l'id : " + id));

        List<AuditDTO> entites = new ArrayList<>();
        entites.addAll(auditService.historiqueEntite("dossier", id.toString()));
        entites.addAll(auditService.historiqueEntite("suivi_dossier", id.toString()));

        entites.sort(Comparator.comparing(
                AuditDTO::getDateAction,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return entites;
    }

    // ════════════════════════════════════════════════════════════
    // Création / modification
    // ═════════════════════════════════════════
    // ════════════════════════════════════════════════════════════

    @Transactional
    public DossierDTO creer(DossierRequest request) {
        if (dossierRepository.existsByNumeroDossier(request.getNumeroDossier())) {
            throw new RuntimeException("Un dossier portant le numéro " + request.getNumeroDossier() + " existe déjà");
        }

        Dossier dossier = Dossier.builder()
                .numeroDossier(request.getNumeroDossier())
                .dateDemande(request.getDateDemande() != null ? request.getDateDemande() : LocalDate.now())
                .annee(request.getAnnee() != null ? request.getAnnee()
                        : (request.getDateDemande() != null ? request.getDateDemande().getYear() : LocalDate.now().getYear()))
                .transmission(request.getTransmission())
                .resultatEtude(request.getResultatEtude())
                .observation(request.getObservation())
                .dateValidation(request.getDateValidation())
                .montantTotal(request.getMontantTotal() != null ? request.getMontantTotal() : BigDecimal.ZERO)
                .statutDossier(resoudreStatut(request.getIdStatutDossier()))
                .typeDossier(resoudreTypeDossier(request.getIdTypeDossier()))
                .dossierFournie(resoudreDossierFournie(request.getDossierFournie()))
                .ville(villeRepository.findById(request.getIdVille())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ville non trouvée avec l'id : " + request.getIdVille())))
                .build();

        Dossier saved = dossierRepository.save(dossier);

        // ── Demandeurs (personnes existantes ou créées à la volée) ──
        enregistrerDemandeurs(saved, request.getDemandeurs());

        // ── Parcelles demandées : superficie + montant calculé (HT/TTC) ──
        enregistrerParcelles(saved, request);

        // ── Étape initiale : la première étape est réalisée à la création ──
        creerPremiereEtape(saved);

        Utilisateur utilisateur = utilisateurConnecte();
        auditService.enregistrer("dossier", saved.getIdDossier().toString(), "CREATE",
                null, Map.<String, Object>of("numeroDossier", saved.getNumeroDossier(),
                             "ville", saved.getVille().getNomVille()),
                utilisateur);
        log.info("Dossier créé : {} (ville={})", saved.getNumeroDossier(), saved.getVille().getNomVille());
        return toDTO(saved);
    }

    @Transactional
    public DossierDTO mettreAJour(Integer id, DossierRequest request) {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier non trouvé avec l'id : " + id));

        // Vérification unicité du numéro si modifié
        if (!dossier.getNumeroDossier().equals(request.getNumeroDossier())
                && dossierRepository.existsByNumeroDossier(request.getNumeroDossier())) {
            throw new RuntimeException("Un dossier portant le numéro " + request.getNumeroDossier() + " existe déjà");
        }

        Map<String, Object> anciennes = snapshot(dossier);

        dossier.setNumeroDossier(request.getNumeroDossier());
        dossier.setDateDemande(request.getDateDemande());
        dossier.setAnnee(request.getAnnee() != null ? request.getAnnee()
                : (request.getDateDemande() != null ? request.getDateDemande().getYear() : null));
        dossier.setTransmission(request.getTransmission());
        dossier.setResultatEtude(request.getResultatEtude());
        dossier.setObservation(request.getObservation());
        dossier.setDateValidation(request.getDateValidation());
        if (request.getMontantTotal() != null) dossier.setMontantTotal(request.getMontantTotal());
        if (request.getIdStatutDossier() != null) {
            dossier.setStatutDossier(resoudreStatut(request.getIdStatutDossier()));
        }
                dossier.setTypeDossier(resoudreTypeDossier(request.getIdTypeDossier()));
                dossier.setDossierFournie(resoudreDossierFournie(request.getDossierFournie()));
        dossier.setVille(villeRepository.findById(request.getIdVille())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ville non trouvée avec l'id : " + request.getIdVille())));
        dossier.setDateModification(LocalDateTime.now());

        Dossier saved = dossierRepository.save(dossier);

        // ── Demandeurs : remplacement complet de la liste ──
        dossierDemandeurRepository.findByIdDossier(id)
                .forEach(dossierDemandeurRepository::delete);
        enregistrerDemandeurs(saved, request.getDemandeurs());

        // ── Parcelles : remplacement complet, montant total recalculé ──
        dossierParcelleRepository.findByDossier_IdDossierOrderByDateAjoutAsc(id)
                .forEach(dossierParcelleRepository::delete);
        enregistrerParcelles(saved, request);

        Utilisateur utilisateur = utilisateurConnecte();
        auditService.enregistrer("dossier", id.toString(), "UPDATE",
                anciennes, snapshot(saved), utilisateur);
        log.info("Dossier mis à jour : {}", saved.getNumeroDossier());
        return toDTO(saved);
    }

    @Transactional
    public void supprimer(Integer id) {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier non trouvé avec l'id : " + id));

        Map<String, Object> anciennes = snapshot(dossier);

        // Suppression en cascade manuelle des enfants
        suiviDossierRepository.findByDossierIdDossierOrderByEtapeOrdreAsc(id)
                .forEach(suiviDossierRepository::delete);
        dossierDemandeurRepository.findByIdDossier(id)
                .forEach(dossierDemandeurRepository::delete);

        // Refusé si des descentes terrain sont rattachées aux lignes dossier_parcelle
        if (descenteTerrainRepository.existsByDossierParcelleDossierIdDossier(id)) {
            throw new RuntimeException(
                    "Impossible de supprimer le dossier " + dossier.getNumeroDossier()
                    + " : des descentes terrain y sont rattachées. Retirez-les d'abord.");
        }
        dossierParcelleRepository.findByDossier_IdDossierOrderByDateAjoutAsc(id)
                .forEach(dossierParcelleRepository::delete);

        dossierRepository.delete(dossier);

        auditService.enregistrer("dossier", id.toString(), "DELETE",
                anciennes, null, utilisateurConnecte());
        log.info("Dossier supprimé : {}", dossier.getNumeroDossier());
    }

    // ════════════════════════════════════════════════════════════
    // Référentiel des étapes — configuration du workflow
    // ════════════════════════════════════════════════════════════

    @Transactional
    public Etape creerEtape(EtapeRequest request) {
        String code = genererCodeEtape(request.getCodeEtape(), request.getLibelle(), null);
        if (etapeRepository.existsByCodeEtape(code)) {
            throw new RuntimeException("Une étape avec le code " + code + " existe déjà");
        }
        if (etapeRepository.existsByOrdre(request.getOrdre())) {
            throw new RuntimeException("Une étape d'ordre " + request.getOrdre() + " existe déjà");
        }

        Etape etape = Etape.builder()
                .codeEtape(code)
                .libelle(request.getLibelle().trim())
                .ordre(request.getOrdre())
                .dureePrevisionnelle(request.getDureePrevisionnelle())
                .build();
        Etape saved = etapeRepository.save(etape);
        auditService.enregistrer("etape", saved.getIdEtape().toString(), "CREATE",
                null, Map.<String, Object>of("code", code, "libelle", saved.getLibelle(), "ordre", saved.getOrdre()),
                utilisateurConnecte());
        log.info("Étape créée : {} (ordre={})", saved.getLibelle(), saved.getOrdre());
        return saved;
    }

    @Transactional
    public Etape mettreAJourEtape(Integer id, EtapeRequest request) {
        Etape etape = etapeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Étape non trouvée avec l'id : " + id));

        String code = genererCodeEtape(request.getCodeEtape(), request.getLibelle(), id);
        etapeRepository.findByCodeEtape(code)
                .filter(e -> !e.getIdEtape().equals(id))
                .ifPresent(e -> {
                    throw new RuntimeException("Une étape avec le code " + code + " existe déjà");
                });
        etapeRepository.findByOrdre(request.getOrdre())
                .filter(e -> !e.getIdEtape().equals(id))
                .ifPresent(e -> {
                    throw new RuntimeException("Une étape d'ordre " + request.getOrdre() + " existe déjà");
                });

        Map<String, Object> anciennes = Map.<String, Object>of(
                "libelle", etape.getLibelle(), "ordre", etape.getOrdre());

        etape.setCodeEtape(code);
        etape.setLibelle(request.getLibelle().trim());
        etape.setOrdre(request.getOrdre());
        etape.setDureePrevisionnelle(request.getDureePrevisionnelle());
        Etape saved = etapeRepository.save(etape);

        auditService.enregistrer("etape", id.toString(), "UPDATE",
                anciennes, Map.<String, Object>of("libelle", saved.getLibelle(), "ordre", saved.getOrdre()),
                utilisateurConnecte());
        log.info("Étape mise à jour : {} (ordre={})", saved.getLibelle(), saved.getOrdre());
        return saved;
    }

    /**
     * Supprime une étape du référentiel. Refusé si l'étape est utilisée
     * dans un suivi de dossier (traçabilité) — dans ce cas, la masquer
     * ou la désactiver est préférable.
     */
    @Transactional
    public void supprimerEtapeReferentiel(Integer id) {
        Etape etape = etapeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Étape non trouvée avec l'id : " + id));
        if (suiviDossierRepository.existsByEtapeIdEtape(id)) {
            throw new RuntimeException("Impossible de supprimer l'étape « " + etape.getLibelle()
                    + " » : elle est utilisée dans le suivi d'un ou plusieurs dossiers.");
        }
        etapeRepository.delete(etape);
        auditService.enregistrer("etape", id.toString(), "DELETE",
                Map.<String, Object>of("code", etape.getCodeEtape(), "libelle", etape.getLibelle()),
                null, utilisateurConnecte());
        log.info("Étape supprimée : {}", etape.getLibelle());
    }

    /** Code majuscule à underscores : fourni, ou dérivé du libellé. */
    private String genererCodeEtape(String codeFourni, String libelle, Integer idExclu) {
        if (codeFourni != null && !codeFourni.isBlank()) {
            return codeFourni.trim().toUpperCase().replaceAll("\\s+", "_");
        }
        String base = libelle.trim()
                .toUpperCase()
                .replaceAll("[^A-Z0-9ÀÂÄÉÈÊËÎÏÔÖÙÛÜÇ]+", "_")
                .replaceAll("^_+|_+$", "");
        if (base.isBlank()) base = "ETAPE";
        String code = base.length() > 50 ? base.substring(0, 50) : base;
        int suffix = 2;
        while (codePris(code, idExclu)) {
            String suf = "_" + suffix++;
            code = (base.length() + suf.length() > 50 ? base.substring(0, 50 - suf.length()) : base) + suf;
        }
        return code;
    }

    /** Code déjà utilisé par une autre étape que idExclu. */
    private boolean codePris(String code, Integer idExclu) {
        return etapeRepository.findByCodeEtape(code)
                .map(e -> !e.getIdEtape().equals(idExclu))
                .orElse(false);
    }

    // ════════════════════════════════════════════════════════════
    // Suivi — changement d'étape
    // ════════════════════════════════════════════════════════════

    /**
     * Enregistre (ou met à jour) une étape du suivi du dossier.
     * La réalisation d'une étape met à jour le statut global du dossier :
     * la dernière étape réalisée → « En cours », la dernière étape du
     * workflow → statut final si le dossier n'a pas déjà un statut final.
     */
    @Transactional
    public DossierDTO.SuiviDossierDTO enregistrerEtape(Integer idDossier, SuiviDossierRequest request) {
        Dossier dossier = dossierRepository.findById(idDossier)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé avec l'id : " + idDossier));

        Etape etape = etapeRepository.findById(request.getIdEtape())
                .orElseThrow(() -> new RuntimeException("Étape non trouvée avec l'id : " + request.getIdEtape()));

        Utilisateur utilisateur = utilisateurConnecte();

        SuiviDossier suivi = suiviDossierRepository
                .findByDossierIdDossierAndEtapeIdEtape(idDossier, etape.getIdEtape())
                .orElseGet(() -> SuiviDossier.builder()
                        .dossier(dossier)
                        .etape(etape)
                        .dateCreation(LocalDateTime.now())
                        .build());

        boolean creation = (suivi.getIdSuivi() == null);
        Map<String, Object> anciennes = creation ? null : snapshotSuivi(suivi);

        suivi.setDateRealisation(request.getDateRealisation() != null ? request.getDateRealisation() : LocalDate.now());
        suivi.setStatutCouleur(request.getIdStatutCouleur() != null
                ? statutCouleurRepository.findById(request.getIdStatutCouleur()).orElse(null) : null);
        suivi.setCommentaire(request.getCommentaire());
        suivi.setUtilisateur(utilisateur);
        suivi.setDateModification(LocalDateTime.now());

        SuiviDossier saved = suiviDossierRepository.save(suivi);

        // ── Statut global du dossier mis à jour automatiquement ──
        mettreAJourStatutDepuisSuivi(dossier, etape, utilisateur);

        auditService.enregistrer("suivi_dossier", String.valueOf(saved.getIdSuivi()), creation ? "CREATE" : "UPDATE",
                anciennes, snapshotSuivi(saved),
                utilisateur);
        log.info("Étape '{}' {} pour le dossier {}", etape.getLibelle(),
                creation ? "enregistrée" : "mise à jour", dossier.getNumeroDossier());
        return toSuiviDTO(saved);
    }

    /** Supprime une étape du suivi (erreur de saisie). */
    @Transactional
    public void supprimerEtape(Integer idDossier, Integer idSuivi) {
        SuiviDossier suivi = suiviDossierRepository.findById(idSuivi)
                .orElseThrow(() -> new RuntimeException("Suivi non trouvé avec l'id : " + idSuivi));
        if (!suivi.getDossier().getIdDossier().equals(idDossier)) {
            throw new RuntimeException("Cette étape n'appartient pas au dossier " + idDossier);
        }
        Map<String, Object> anciennes = snapshotSuivi(suivi);
        suiviDossierRepository.delete(suivi);
        auditService.enregistrer("suivi_dossier", idSuivi.toString(), "DELETE",
                anciennes, null, utilisateurConnecte());
    }

    // ════════════════════════════════════════════════════════════
    // Statistiques
    // ════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Map<String, Object> statistiques() {
        long total = dossierRepository.count();
        long finalises = dossierRepository.countDossiersFinalises();
        List<Object[]> parStatut = dossierRepository.countParStatut();

        List<Map<String, Object>> repartition = parStatut.stream()
                .map(row -> Map.<String, Object>of(
                        "statut", (String) row[0],
                        "nombre", (Long) row[1]))
                .toList();

        int anneeCourante = LocalDate.now().getYear();
        long anneeEnCours = dossierRepository.countByAnnee(anneeCourante);

        return Map.of(
                "total", total,
                "finalises", finalises,
                "enCours", total - finalises,
                "anneeEnCours", anneeEnCours,
                "parStatut", repartition);
    }

    // ════════════════════════════════════════════════════════════
    // Helpers privés
    // ═════════════════════════════════════════════════ 
    // ════════════════════════════════════════════════════════════

    // ════════════════════════════════════════════════════════════
    // Parcelles demandées — superficie, prix au m², HT/TTC
    // ════════════════════════════════════════════════════════════

    /**
     * Enregistre les parcelles demandées du dossier et recalcule le montant
     * total (somme des TTC) selon le prix au m² de la ville :
     *  - superficie renseignée, sinon celle de la parcelle, sinon celle de la propriété ;
     *  - montants HT/TTC toujours calculés = superficie × dernier prix au m² de la ville.
     */
    private void enregistrerParcelles(Dossier dossier, DossierRequest request) {
        List<DossierRequest.ParcelleDemandeeRequest> lignes = request.getParcelles();
        if (lignes == null || lignes.isEmpty()) return;

        PrixM2 prix = dernierPrixVille(dossier.getVille().getIdVille());

        BigDecimal totalTtc = BigDecimal.ZERO;
        List<DossierParcelle> entities = new ArrayList<>();

        for (DossierRequest.ParcelleDemandeeRequest pr : lignes) {
            Propriete propriete = proprieteRepository.findById(pr.getIdPropriete())
                    .orElseThrow(() -> new RuntimeException(
                            "Propriété non trouvée avec l'id : " + pr.getIdPropriete()));

            Parcelle parcelle = null;
            if (pr.getIdParcelle() != null) {
                parcelle = parcelleRepository.findById(pr.getIdParcelle())
                        .orElseThrow(() -> new RuntimeException(
                                "Parcelle non trouvée avec l'id : " + pr.getIdParcelle()));
            }

            // ── Superficie demandée : saisie > parcelle > propriété ──
            BigDecimal superficie = pr.getSuperficieM2() != null ? pr.getSuperficieM2()
                    : (parcelle != null && parcelle.getSuperficieM2() != null ? parcelle.getSuperficieM2()
                    : (propriete.getSuperficieTotale() != null ? propriete.getSuperficieTotale() : null));

            // ── Montants : toujours calculés = superficie × prix au m² de la ville ──
            BigDecimal ttc = BigDecimal.ZERO;
            if (superficie != null && prix != null && prix.getPrixTtc() != null) {
                ttc = superficie.multiply(prix.getPrixTtc()).setScale(2, java.math.RoundingMode.HALF_UP);
            }

            totalTtc = totalTtc.add(ttc);

            entities.add(DossierParcelle.builder()
                    .idDossierParcelle(java.util.UUID.randomUUID())
                    .dossier(dossier)
                    .propriete(propriete)
                    .parcelle(parcelle)
                    .superficieM2(superficie)
                    .observation(pr.getObservation())
                    .build());
        }

        dossierParcelleRepository.saveAll(entities);

        if (totalTtc.signum() > 0) {
            dossier.setMontantTotal(totalTtc);
            dossierRepository.save(dossier);
        }
    }

    /** Dernier prix au m² connu pour la ville (date_maj la plus récente). */
    private PrixM2 dernierPrixVille(Integer idVille) {
        List<PrixM2> prix = prixM2Repository.findByVilleIdVilleOrderByDateMajDesc(idVille);
        return prix.isEmpty() ? null : prix.get(0);
    }

    private DossierDTO.ParcelleDemandeeDTO toParcelleDTO(DossierParcelle dp) {
        return DossierDTO.ParcelleDemandeeDTO.builder()
                .idDossierParcelle(dp.getIdDossierParcelle())
                .idPropriete(dp.getPropriete() != null ? dp.getPropriete().getIdPropriete() : null)
                .nomPropriete(dp.getPropriete() != null ? dp.getPropriete().getNom() : null)
                .numeroPropriete(dp.getPropriete() != null ? dp.getPropriete().getNumero() : null)
                .idParcelle(dp.getParcelle() != null ? dp.getParcelle().getIdParcelle() : null)
                .numeroLot(dp.getParcelle() != null ? dp.getParcelle().getNumeroLot() : null)
                .superficieM2(dp.getSuperficieM2())
                .observation(dp.getObservation())
                .build();
    }

    private StatutDossier resoudreStatut(Integer idStatut) {
        if (idStatut != null) {
            return statutDossierRepository.findById(idStatut)
                    .orElseThrow(() -> new RuntimeException("Statut dossier non trouvé avec l'id : " + idStatut));
        }
        return statutDossierRepository.findAll().stream()
                .filter(s -> "En cours".equals(s.getCode()))
                .findFirst()
                .orElse(null);
    }

        private TypeDossier resoudreTypeDossier(Integer idTypeDossier) {
                if (idTypeDossier == null) return null;
                return typeDossierRepository.findById(idTypeDossier)
                                .orElseThrow(() -> new RuntimeException("Type de dossier non trouvé avec l'id : " + idTypeDossier));
        }

        private DossierFournie resoudreDossierFournie(String libelle) {
                if (libelle == null || libelle.isBlank()) return null;
                String libelleNormalise = libelle.trim();
                return dossierFournieRepository.findByLibelleIgnoreCase(libelleNormalise)
                                .orElseGet(() -> dossierFournieRepository.save(
                                                DossierFournie.builder().libelle(libelleNormalise).build()));
        }

    private void enregistrerDemandeurs(Dossier dossier, List<DossierRequest.DemandeurRequest> demandeurs) {
        if (demandeurs == null || demandeurs.isEmpty()) return;

        for (DossierRequest.DemandeurRequest dr : demandeurs) {
            Personne personne;
            if (dr.getIdPersonne() != null) {
                personne = personneRepository.findById(dr.getIdPersonne())
                        .orElseThrow(() -> new RuntimeException(
                                "Personne non trouvée avec l'id : " + dr.getIdPersonne()));
            } else {
                if (dr.getNom() == null || dr.getNom().isBlank()) {
                    throw new RuntimeException("Le nom du demandeur est obligatoire");
                }
                // Déduplication : réutilise la personne si elle existe déjà (même nom)
                personne = personneRepository.findFirstByNomIgnoreCase(dr.getNom().trim())
                        .orElseGet(() -> {
                            TypePersonne typeDemandeur = typePersonneRepository.findByCode("DEMANDEUR")
                                    .orElse(null);
                            Personne p = Personne.builder()
                                    .nom(dr.getNom().trim())
                                    .contact(dr.getContact())
                                    .email(dr.getEmail())
                                    .adresse(dr.getAdresse())
                                    .role("Demandeur")
                                    .typePersonne(typeDemandeur)
                                    .build();
                            return personneRepository.save(p);
                        });
            }
            if (!dossierDemandeurRepository.existsByIdDossierAndIdPersonne(
                    dossier.getIdDossier(), personne.getIdPersonne())) {
                DossierDemandeur dd = DossierDemandeur.builder()
                        .idDossier(dossier.getIdDossier())
                        .idPersonne(personne.getIdPersonne())
                        // Référence obligatoire : toDTO lit dd.getPersonne() dans
                        // la même transaction (pas de rechargement depuis la base).
                        .personne(personne)
                        .role(dr.getRole() != null && !dr.getRole().isBlank() ? dr.getRole() : "Principal")
                        .build();
                dossierDemandeurRepository.save(dd);
            }
        }
    }

    /** À la création du dossier, la première étape du workflow est marquée réalisée. */
    private void creerPremiereEtape(Dossier dossier) {
        List<Etape> etapes = etapeRepository.findAllByOrderByOrdreAsc();
        if (etapes.isEmpty()) return;
        Etape premiere = etapes.get(0);
        if (suiviDossierRepository.existsByDossierIdDossierAndEtapeIdEtape(
                dossier.getIdDossier(), premiere.getIdEtape())) return;

        suiviDossierRepository.save(SuiviDossier.builder()
                .dossier(dossier)
                .etape(premiere)
                .dateRealisation(dossier.getDateDemande() != null ? dossier.getDateDemande() : LocalDate.now())
                .commentaire("Dépôt de la demande")
                .utilisateur(utilisateurConnecte())
                .build());
    }

    /**
     * Met à jour le statut du dossier selon l'étape enregistrée :
     *  - si l'étape est la dernière du workflow → statut final (si le dossier
     *    n'en a pas déjà un) ;
     *  - sinon → « En cours » si le dossier n'a pas de statut final.
     */
    private void mettreAJourStatutDepuisSuivi(Dossier dossier, Etape etapeRealisee, Utilisateur utilisateur) {
        List<Etape> etapes = etapeRepository.findAllByOrderByOrdreAsc();
        if (etapes.isEmpty()) return;
        Etape derniere = etapes.get(etapes.size() - 1);

        boolean etapeFinale = etapeRealisee.getIdEtape().equals(derniere.getIdEtape());
        StatutDossier statutActuel = dossier.getStatutDossier();
        boolean statutFinalActuel = statutActuel != null && Boolean.TRUE.equals(statutActuel.getEstFinal());

        String cible = null;
        if (etapeFinale && !statutFinalActuel) {
            cible = "Termine";
        } else if (!etapeFinale && !statutFinalActuel) {
            cible = "En cours";
        }
        if (cible == null) return;
                String statutCible = cible;

                statutDossierRepository.findByCode(statutCible).ifPresent(statut -> {
            dossier.setStatutDossier(statut);
                        if ("Termine".equals(statutCible) && dossier.getDateValidation() == null) {
                dossier.setDateValidation(LocalDate.now());
            }
            dossier.setDateModification(LocalDateTime.now());
            dossierRepository.save(dossier);
            auditService.enregistrer("dossier", dossier.getIdDossier().toString(), "UPDATE",
                    Map.<String, Object>of("statut", statutActuel != null ? statutActuel.getLibelle() : "—"),
                    Map.<String, Object>of("statut", statut.getLibelle()),
                    utilisateur);
        });
    }

    /** DTO complet d'un dossier (statut, type, demandeurs, parcelles, suivi) — réutilisé par les autres modules (ex. détail d'une descente terrain). */
    public DossierDTO toDTO(Dossier dossier) {
        List<DossierDTO.DemandeurDTO> demandeurs = dossierDemandeurRepository
                .findByIdDossier(dossier.getIdDossier()).stream()
                .map(dd -> {
                    // Entité liée null si la ligne vient d'être créée dans cette
                    // transaction : recharger depuis la base pour éviter le NPE.
                    Personne p = dd.getPersonne() != null
                            ? dd.getPersonne()
                            : personneRepository.findById(dd.getIdPersonne()).orElse(null);
                    if (p == null) return null;
                    return DossierDTO.DemandeurDTO.builder()
                            .idPersonne(p.getIdPersonne())
                            .nom(p.getNom())
                            .contact(p.getContact())
                            .email(p.getEmail())
                            .adresse(p.getAdresse())
                            .role(dd.getRole())
                            .codeTypePersonne(p.getTypePersonne() != null ? p.getTypePersonne().getCode() : null)
                            .libelleTypePersonne(p.getTypePersonne() != null ? p.getTypePersonne().getLibelle() : null)
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        List<DossierDTO.SuiviDossierDTO> suivi = suiviDossierRepository
                .findByDossierIdDossierOrderByEtapeOrdreAsc(dossier.getIdDossier())
                .stream().map(this::toSuiviDTO)
                .toList();

        List<DossierDTO.ParcelleDemandeeDTO> parcelles = dossierParcelleRepository
                .findByDossier_IdDossierOrderByDateAjoutAsc(dossier.getIdDossier())
                .stream().map(this::toParcelleDTO)
                .toList();

        // ── Étape actuelle = prochaine étape non réalisée ──
        DossierDTO.SuiviDossierDTO etapeActuelle = suivi.stream()
                .filter(s -> s.getDateRealisation() == null)
                .findFirst().orElse(null);
        // (toutes réalisées → dossier arrivé au bout du workflow)

        return DossierDTO.builder()
                .idDossier(dossier.getIdDossier())
                .numeroDossier(dossier.getNumeroDossier())
                .dateDemande(dossier.getDateDemande())
                .annee(dossier.getAnnee())
                .transmission(dossier.getTransmission())
                .resultatEtude(dossier.getResultatEtude())
                .observation(dossier.getObservation())
                .dateCreation(dossier.getDateCreation())
                .dateModification(dossier.getDateModification())
                .dateValidation(dossier.getDateValidation())
                .montantTotal(dossier.getMontantTotal())
                .idStatutDossier(dossier.getStatutDossier() != null ? dossier.getStatutDossier().getIdStatutDossier() : null)
                .codeStatut(dossier.getStatutDossier() != null ? dossier.getStatutDossier().getCode() : null)
                .libelleStatut(dossier.getStatutDossier() != null ? dossier.getStatutDossier().getLibelle() : null)
                .couleurStatutHex(dossier.getStatutDossier() != null ? dossier.getStatutDossier().getCouleurHex() : null)
                .statutFinal(dossier.getStatutDossier() != null ? dossier.getStatutDossier().getEstFinal() : null)
                .idTypeDossier(dossier.getTypeDossier() != null ? dossier.getTypeDossier().getIdTypeDossier() : null)
                .libelleTypeDossier(dossier.getTypeDossier() != null ? dossier.getTypeDossier().getLibelle() : null)
                .idDossierFournie(dossier.getDossierFournie() != null ? dossier.getDossierFournie().getIdDossierFournie() : null)
                .libelleDossierFournie(dossier.getDossierFournie() != null ? dossier.getDossierFournie().getLibelle() : null)
                .idVille(dossier.getVille().getIdVille())
                .nomVille(dossier.getVille().getNomVille())
                .demandeurs(demandeurs)
                .parcelles(parcelles)
                .suivi(suivi)
                .etapeActuelleOrdre(etapeActuelle != null ? etapeActuelle.getOrdreEtape() : null)
                .etapeActuelleLibelle(etapeActuelle != null ? etapeActuelle.getLibelleEtape() : null)
                .nombreEtapesRealisees((int) suivi.stream().filter(s -> s.getDateRealisation() != null).count())
                .build();
    }

    private DossierDTO.SuiviDossierDTO toSuiviDTO(SuiviDossier s) {
        return DossierDTO.SuiviDossierDTO.builder()
                .idSuivi(s.getIdSuivi())
                .idEtape(s.getEtape().getIdEtape())
                .codeEtape(s.getEtape().getCodeEtape())
                .libelleEtape(s.getEtape().getLibelle())
                .ordreEtape(s.getEtape().getOrdre())
                .dureePrevisionnelle(s.getEtape().getDureePrevisionnelle())
                .dateRealisation(s.getDateRealisation())
                .idStatutCouleur(s.getStatutCouleur() != null ? s.getStatutCouleur().getIdStatutCouleur() : null)
                .codeStatutCouleur(s.getStatutCouleur() != null ? s.getStatutCouleur().getCode() : null)
                .libelleStatutCouleur(s.getStatutCouleur() != null ? s.getStatutCouleur().getLibelle() : null)
                .couleurStatutCouleurHex(s.getStatutCouleur() != null ? s.getStatutCouleur().getCouleurHex() : null)
                .commentaire(s.getCommentaire())
                .nomUtilisateur(s.getUtilisateur() != null ? s.getUtilisateur().getNomComplet() : null)
                .dateCreation(s.getDateCreation())
                .dateModification(s.getDateModification())
                .build();
    }

    private Map<String, Object> snapshot(Dossier d) {
        Map<String, Object> map = new HashMap<>();
        map.put("numeroDossier", d.getNumeroDossier());
        map.put("dateDemande", d.getDateDemande() != null ? d.getDateDemande().toString() : null);
        map.put("transmission", d.getTransmission());
        map.put("resultatEtude", d.getResultatEtude());
        map.put("observation", d.getObservation());
        map.put("dateValidation", d.getDateValidation() != null ? d.getDateValidation().toString() : null);
        map.put("montantTotal", d.getMontantTotal() != null ? d.getMontantTotal().toString() : null);
        map.put("statut", d.getStatutDossier() != null ? d.getStatutDossier().getLibelle() : null);
        map.put("typeDossier", d.getTypeDossier() != null ? d.getTypeDossier().getLibelle() : null);
        map.put("dossierFournie", d.getDossierFournie() != null ? d.getDossierFournie().getLibelle() : null);
        map.put("ville", d.getVille() != null ? d.getVille().getNomVille() : null);
        return map;
    }

    private Map<String, Object> snapshotSuivi(SuiviDossier s) {
        Map<String, Object> map = new HashMap<>();
        map.put("etape", s.getEtape() != null ? s.getEtape().getLibelle() : null);
        map.put("dateRealisation", s.getDateRealisation() != null ? s.getDateRealisation().toString() : null);
        map.put("statutCouleur", s.getStatutCouleur() != null ? s.getStatutCouleur().getLibelle() : null);
        map.put("commentaire", s.getCommentaire());
        return map;
    }

    private Utilisateur utilisateurConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getPrincipal())) return null;
        return utilisateurRepository.findByNomUtilisateur(auth.getName()).orElse(null);
    }
}
