package com.seimad.patrimoine.service.descente;

import com.seimad.patrimoine.dto.carte.GeometrieRequest;
import com.seimad.patrimoine.dto.descente.DescenteTerrainDTO;
import com.seimad.patrimoine.dto.descente.DescenteTerrainRequest;
import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.descente.DescenteTerrain;
import com.seimad.patrimoine.entity.dossier.DossierParcelle;
import com.seimad.patrimoine.entity.notification.Personne;
import com.seimad.patrimoine.repository.auth.UtilisateurRepository;
import com.seimad.patrimoine.repository.carte.GeometrieRepository;
import com.seimad.patrimoine.repository.descente.DescenteTerrainRepository;
import com.seimad.patrimoine.repository.dossier.DossierParcelleRepository;
import com.seimad.patrimoine.repository.notification.PersonneRepository;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class DescenteTerrainService {

    private final DescenteTerrainRepository descenteTerrainRepository;
    private final DossierParcelleRepository dossierParcelleRepository;
    private final PersonneRepository personneRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final GeometrieRepository geometrieRepository;
    private final AuditService auditService;

    @PersistenceContext
    private EntityManager entityManager;

    // ── CRUD ──

    @Transactional(readOnly = true)
    public Page<DescenteTerrainDTO> lister(Pageable pageable) {
        return descenteTerrainRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<DescenteTerrainDTO> rechercher(String search, Pageable pageable) {
        return descenteTerrainRepository.search(search, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public DescenteTerrainDTO trouverParId(UUID id) {
        DescenteTerrain dt = descenteTerrainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Descente terrain non trouvée avec l'id : " + id));
        return toDTO(dt);
    }

    @Transactional
    public DescenteTerrainDTO creer(DescenteTerrainRequest request) {
        Personne personne = resoudrePersonne(request.getIdPersonne());
        DossierParcelle dossierParcelle = resoudreDossierParcelle(request.getIdDossierParcelle());
        Utilisateur utilisateurCreation = resoudreUtilisateur(request.getIdUtilisateurCreation());

        DescenteTerrain dt = DescenteTerrain.builder()
                .idDescente(UUID.randomUUID())
                .reference(resoudreReference(request))
                .dateDescente(request.getDateDescente())
                .statutConstat(request.getStatutConstat() != null ? request.getStatutConstat() : "En attente")
                .observation(request.getObservation())
                .mode(request.getMode() != null ? request.getMode() : "online")
                .validation(request.getValidation() != null ? request.getValidation() : "En attente")
                .personne(personne)
                .dossierParcelle(dossierParcelle)
                .utilisateurCreation(utilisateurCreation)
                .build();

        dt.setDateModification(LocalDateTime.now());

        DescenteTerrain saved = descenteTerrainRepository.save(dt);

        if (request.getGeometrie() != null) {
            enregistrerGeometrie(request.getGeometrie(), saved.getIdDescente(), saved.getUtilisateurCreation());
        }

        auditService.enregistrer("descente_terrain", saved.getIdDescente().toString(), "CREATE",
                null, valeursAudit(saved));
        return toDTO(saved);
    }

    @Transactional
    public DescenteTerrainDTO mettreAJour(UUID id, DescenteTerrainRequest request) {
        DescenteTerrain dt = descenteTerrainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Descente terrain non trouvée avec l'id : " + id));

        Map<String, Object> anciennesValeurs = valeursAudit(dt);

        if (request.getDateDescente() != null) dt.setDateDescente(request.getDateDescente());
        if (request.getStatutConstat() != null) dt.setStatutConstat(request.getStatutConstat());
        if (request.getObservation() != null) dt.setObservation(request.getObservation());
        if (request.getMode() != null) dt.setMode(request.getMode());
        if (request.getValidation() != null) dt.setValidation(request.getValidation());
        if (request.getIdPersonne() != null) dt.setPersonne(resoudrePersonne(request.getIdPersonne()));
        if (request.getIdDossierParcelle() != null) dt.setDossierParcelle(resoudreDossierParcelle(request.getIdDossierParcelle()));

        dt.setDateModification(LocalDateTime.now());

        DescenteTerrain saved = descenteTerrainRepository.save(dt);

        if (request.getGeometrie() != null) {
            enregistrerGeometrie(request.getGeometrie(), id, saved.getUtilisateurCreation());
        }

        auditService.enregistrer("descente_terrain", id.toString(), "UPDATE",
                anciennesValeurs, valeursAudit(saved));
        return toDTO(saved);
    }

    @Transactional
    public void supprimer(UUID id) {
        DescenteTerrain dt = descenteTerrainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Descente terrain non trouvée avec l'id : " + id));

        Map<String, Object> anciennesValeurs = valeursAudit(dt);
        auditService.enregistrer("descente_terrain", id.toString(), "DELETE", anciennesValeurs, null);
        descenteTerrainRepository.deleteById(id);
        geometrieRepository.supprimerParEntite("descente_terrain", id);
    }

    // ── Statistiques ──

    @Transactional(readOnly = true)
    public Map<String, Object> statistiques() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", descenteTerrainRepository.count());
        return stats;
    }

    // ── Recherche de dossiers (autocomplete mobile) ──

    @Transactional(readOnly = true)
    public List<Map<String, Object>> rechercherDossiers(String search) {
        List<DossierParcelle> results = dossierParcelleRepository.searchDossiers(search);
        return results.stream().map(dp -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("idDossierParcelle", dp.getIdDossierParcelle());
            map.put("numeroDossier", dp.getDossier() != null ? dp.getDossier().getNumeroDossier() : null);
            map.put("superficie", dp.getSuperficieM2());
            map.put("parcelle", dp.getParcelle() != null ? dp.getParcelle().getNumeroLot() : null);
            map.put("ville", dp.getPropriete() != null && dp.getPropriete().getVille() != null
                    ? dp.getPropriete().getVille().getNomVille() : null);
            map.put("propriete", dp.getPropriete() != null ? dp.getPropriete().getNumero() : null);

            try {
                Object[] demandeur = (Object[]) entityManager.createNativeQuery(
                        "SELECT dd.id_personne, p.nom, p.contact " +
                        "FROM dossier_demandeur dd " +
                        "JOIN personne p ON p.id_personne = dd.id_personne " +
                        "WHERE dd.id_dossier = :idDossier AND dd.role = 'Principal' " +
                        "LIMIT 1")
                        .setParameter("idDossier", dp.getDossier().getIdDossier())
                        .getSingleResult();
                if (demandeur != null) {
                    map.put("demandeurNom", demandeur[1]);
                    map.put("demandeurContact", demandeur[2]);
                }
            } catch (Exception e) {
                // Pas de demandeur principal trouvé
            }

            return map;
        }).toList();
    }

    // ── Helpers ──

    private String resoudreReference(DescenteTerrainRequest request) {
        if (request.getReference() != null && !request.getReference().isBlank()) {
            return request.getReference();
        }
        int annee = LocalDate.now().getYear();
        String prefix = "DRT-" + annee + "-";
        Long max = descenteTerrainRepository.maxNumeroReference(prefix, prefix.length() + 1);
        return prefix + String.format("%04d", (max == null ? 0 : max) + 1);
    }

    private Personne resoudrePersonne(Integer idPersonne) {
        return idPersonne != null ? personneRepository.findById(idPersonne)
                .orElseThrow(() -> new RuntimeException("Personne non trouvée avec l'id : " + idPersonne)) : null;
    }

    private DossierParcelle resoudreDossierParcelle(UUID idDossierParcelle) {
        return idDossierParcelle != null ? dossierParcelleRepository.findById(idDossierParcelle)
                .orElseThrow(() -> new RuntimeException("DossierParcelle non trouvé avec l'id : " + idDossierParcelle)) : null;
    }

    private Utilisateur resoudreUtilisateur(Integer idUtilisateur) {
        if (idUtilisateur != null) {
            return utilisateurRepository.findById(idUtilisateur)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id : " + idUtilisateur));
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getPrincipal())) return null;
        return utilisateurRepository.findByNomUtilisateur(auth.getName()).orElse(null);
    }

    private void enregistrerGeometrie(GeometrieRequest geometrie, UUID idDescente, Utilisateur utilisateur) {
        if (geometrie == null || geometrie.getGeojson() == null || geometrie.getGeojson().isBlank()) return;
        if (utilisateur == null) {
            log.warn("Géométrie ignorée pour la descente {} : utilisateur non résolu", idDescente);
            return;
        }
        String type = geometrie.getTypeGeometrie() != null ? geometrie.getTypeGeometrie() : "Point";
        String source = geometrie.getSource() != null && !geometrie.getSource().isBlank()
                ? geometrie.getSource() : "Point carte";
        BigDecimal precisionM = geometrie.getPrecisionM() != null
                ? BigDecimal.valueOf(geometrie.getPrecisionM()) : null;
        geometrieRepository.upsert("descente_terrain", idDescente, type, geometrie.getGeojson(),
                precisionM, source, utilisateur.getIdUtilisateur());
        log.info("Géométrie {} enregistrée pour la descente {}", type, idDescente);
    }

    private Map<String, Object> valeursAudit(DescenteTerrain dt) {
        Map<String, Object> valeurs = new LinkedHashMap<>();
        valeurs.put("reference", dt.getReference());
        valeurs.put("dateDescente", dt.getDateDescente());
        valeurs.put("statutConstat", dt.getStatutConstat());
        valeurs.put("observation", dt.getObservation());
        valeurs.put("mode", dt.getMode());
        valeurs.put("validation", dt.getValidation());
        valeurs.put("personne", dt.getPersonne() != null ? dt.getPersonne().getNom() : null);
        valeurs.put("numeroDossier", dt.getDossierParcelle() != null
                && dt.getDossierParcelle().getDossier() != null
                ? dt.getDossierParcelle().getDossier().getNumeroDossier() : null);
        valeurs.put("utilisateurCreation",
                dt.getUtilisateurCreation() != null ? dt.getUtilisateurCreation().getNomComplet() : null);
        return valeurs;
    }

    private DescenteTerrainDTO toDTO(DescenteTerrain dt) {
        DescenteTerrainDTO.DescenteTerrainDTOBuilder builder = DescenteTerrainDTO.builder()
                .idDescente(dt.getIdDescente())
                .reference(dt.getReference())
                .dateDescente(dt.getDateDescente())
                .statutConstat(dt.getStatutConstat())
                .observation(dt.getObservation())
                .mode(dt.getMode())
                .validation(dt.getValidation())
                .dateValidation(dt.getDateValidation())
                .synchronise(dt.getSynchronise())
                .createdAt(dt.getCreatedAt())
                .dateModification(dt.getDateModification());

        if (dt.getUtilisateurCreation() != null) {
            builder.idUtilisateurCreation(dt.getUtilisateurCreation().getIdUtilisateur());
            builder.nomUtilisateurCreation(dt.getUtilisateurCreation().getNomComplet());
        }
        if (dt.getUtilisateurValidation() != null) {
            builder.idUtilisateurValidation(dt.getUtilisateurValidation().getIdUtilisateur());
            builder.nomUtilisateurValidation(dt.getUtilisateurValidation().getNomComplet());
        }
        if (dt.getPersonne() != null) {
            builder.idPersonne(dt.getPersonne().getIdPersonne());
            builder.nomPersonne(dt.getPersonne().getNom());
            builder.contactPersonne(dt.getPersonne().getContact());
        }
        if (dt.getDossierParcelle() != null) {
            builder.idDossierParcelle(dt.getDossierParcelle().getIdDossierParcelle());
            builder.superficieM2(dt.getDossierParcelle().getSuperficieM2());

            if (dt.getDossierParcelle().getDossier() != null) {
                builder.idDossier(dt.getDossierParcelle().getDossier().getIdDossier());
                builder.numeroDossier(dt.getDossierParcelle().getDossier().getNumeroDossier());
                if (dt.getDossierParcelle().getDossier().getVille() != null) {
                    builder.idVille(dt.getDossierParcelle().getDossier().getVille().getIdVille());
                    builder.nomVille(dt.getDossierParcelle().getDossier().getVille().getNomVille());
                }
            }
            if (dt.getDossierParcelle().getPropriete() != null) {
                builder.idPropriete(dt.getDossierParcelle().getPropriete().getIdPropriete());
                builder.numeroPropriete(dt.getDossierParcelle().getPropriete().getNumero());
            }
            if (dt.getDossierParcelle().getParcelle() != null) {
                builder.idParcelle(dt.getDossierParcelle().getParcelle().getIdParcelle());
                builder.numeroLot(dt.getDossierParcelle().getParcelle().getNumeroLot());
            }
        }

        return builder.build();
    }
}
