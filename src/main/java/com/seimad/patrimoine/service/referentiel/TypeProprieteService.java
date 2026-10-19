package com.seimad.patrimoine.service.referentiel;

import com.seimad.patrimoine.dto.referentiel.TypeProprieteDTO;
import com.seimad.patrimoine.dto.referentiel.TypeProprieteRequest;
import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.referentiel.TypePropriete;
import com.seimad.patrimoine.repository.auth.UtilisateurRepository;
import com.seimad.patrimoine.repository.referentiel.TypeProprieteRepository;
import com.seimad.patrimoine.service.dossier.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TypeProprieteService {

    private final TypeProprieteRepository typeProprieteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<TypeProprieteDTO> lister() {
        return typeProprieteRepository.findAllByOrderByLibelleAsc().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public TypeProprieteDTO trouverParId(Integer id) {
        TypePropriete type = typeProprieteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Type de propriété non trouvé avec l'id : " + id));
        return toDTO(type);
    }

    @Transactional
    public TypeProprieteDTO creer(TypeProprieteRequest request) {
        String libelle = request.getLibelle().trim();
        typeProprieteRepository.findByLibelleIgnoreCase(libelle).ifPresent(existing -> {
            throw new RuntimeException("Le type de propriété « " + libelle + " » existe déjà");
        });
        TypePropriete saved = typeProprieteRepository.save(
                TypePropriete.builder().libelle(libelle).build());
        auditService.enregistrer("type_propriete", saved.getIdTypePropriete().toString(),
                "CREATE", null,
                Map.<String, Object>of("libelle", libelle),
                utilisateurConnecte());
        log.info("Type de propriété créé : {}", libelle);
        return toDTO(saved);
    }

    @Transactional
    public TypeProprieteDTO mettreAJour(Integer id, TypeProprieteRequest request) {
        TypePropriete type = typeProprieteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Type de propriété non trouvé avec l'id : " + id));
        String libelle = request.getLibelle().trim();
        typeProprieteRepository.findByLibelleIgnoreCase(libelle)
                .filter(existing -> !existing.getIdTypePropriete().equals(id))
                .ifPresent(existing -> {
                    throw new RuntimeException("Le type de propriété « " + libelle + " » existe déjà");
                });
        Map<String, Object> anciennes = Map.<String, Object>of("libelle", type.getLibelle());
        type.setLibelle(libelle);
        TypePropriete saved = typeProprieteRepository.save(type);
        auditService.enregistrer("type_propriete", id.toString(), "UPDATE", anciennes,
                Map.<String, Object>of("libelle", libelle),
                utilisateurConnecte());
        log.info("Type de propriété mis à jour : {} (id={})", libelle, id);
        return toDTO(saved);
    }

    @Transactional
    public void supprimer(Integer id) {
        TypePropriete type = typeProprieteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Type de propriété non trouvé avec l'id : " + id));
        // Vérification : le type n'est pas utilisé par des propriétés existantes
        long utilisation = typeProprieteRepository.countByTypeUtilise(id);
        if (utilisation > 0) {
            throw new RuntimeException("Impossible de supprimer le type « " + type.getLibelle()
                    + " » : il est utilisé par " + utilisation + " propriété(s).");
        }
        Map<String, Object> anciennes = Map.<String, Object>of("libelle", type.getLibelle());
        typeProprieteRepository.delete(type);
        auditService.enregistrer("type_propriete", id.toString(), "DELETE", anciennes, null,
                utilisateurConnecte());
        log.info("Type de propriété supprimé : {} (id={})", type.getLibelle(), id);
    }

    @Transactional(readOnly = true)
    public TypeProprieteDTO trouverParDefaut() {
        TypePropriete type = typeProprieteRepository.findFirstByLibelleIgnoreCase("Indéterminé")
                .orElseThrow(() -> new RuntimeException(
                        "Le type de propriété par défaut « Indéterminé » est introuvable"));
        return toDTO(type);
    }

    private TypeProprieteDTO toDTO(TypePropriete t) {
        return TypeProprieteDTO.builder()
                .idTypePropriete(t.getIdTypePropriete())
                .libelle(t.getLibelle())
                .build();
    }

    private Utilisateur utilisateurConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return utilisateurRepository.findByNomUtilisateur(auth.getName()).orElse(null);
    }
}
