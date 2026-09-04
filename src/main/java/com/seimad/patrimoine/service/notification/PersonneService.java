package com.seimad.patrimoine.service.notification;

import com.seimad.patrimoine.dto.notification.PersonneDTO;
import com.seimad.patrimoine.dto.notification.PersonneRequest;
import com.seimad.patrimoine.entity.notification.Personne;
import com.seimad.patrimoine.entity.notification.TypePersonne;
import com.seimad.patrimoine.repository.notification.PersonneRepository;
import com.seimad.patrimoine.repository.notification.TypePersonneRepository;
import com.seimad.patrimoine.service.dossier.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PersonneService {

    private final PersonneRepository personneRepository;
    private final TypePersonneRepository typePersonneRepository;
    private final AuditService auditService;

    // ── Référentiel type ──

    @Transactional(readOnly = true)
    public List<TypePersonne> listerTypes() {
        return typePersonneRepository.findAllByOrderByLibelleAsc();
    }

    // ── CRUD ──

    @Transactional(readOnly = true)
    public Page<PersonneDTO> lister(Pageable pageable) {
        return personneRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<PersonneDTO> rechercher(String search, Pageable pageable) {
        return personneRepository.search(search, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public PersonneDTO trouverParId(Integer id) {
        Personne personne = personneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Personne non trouvée avec l'id : " + id));
        return toDTO(personne);
    }

    @Transactional
    public PersonneDTO creer(PersonneRequest request) {
        TypePersonne type = trouverType(request.getIdTypePersonne());
        Personne personne = Personne.builder()
                .nom(request.getNom())
                .contact(request.getContact())
                .adresse(request.getAdresse())
                .email(request.getEmail())
                .role(type != null ? type.getLibelle() : request.getRole())
                .typePersonne(type)
                .build();
        // @PrePersist on entity set date = LocalDateTime.now() if null
        return toDTO(personneRepository.save(personne));
    }

    @Transactional
    public PersonneDTO mettreAJour(Integer id, PersonneRequest request) {
        Personne personne = personneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Personne non trouvée avec l'id : " + id));

        TypePersonne type = trouverType(request.getIdTypePersonne());

        // ── Enregistrer les anciennes valeurs AVANT modification ──
        Map<String, Object> anciennesValeurs = new LinkedHashMap<>();
        anciennesValeurs.put("nom", personne.getNom());
        anciennesValeurs.put("contact", personne.getContact());
        anciennesValeurs.put("email", personne.getEmail());
        anciennesValeurs.put("adresse", personne.getAdresse());
        anciennesValeurs.put("role", personne.getRole());
        anciennesValeurs.put("typePersonne", personne.getTypePersonne() != null
                ? personne.getTypePersonne().getLibelle() : null);

        // ── Appliquer les nouvelles valeurs ──
        Map<String, Object> nouvellesValeurs = new LinkedHashMap<>();
        nouvellesValeurs.put("nom", request.getNom());
        nouvellesValeurs.put("contact", request.getContact());
        nouvellesValeurs.put("email", request.getEmail());
        nouvellesValeurs.put("adresse", request.getAdresse());
        nouvellesValeurs.put("role", type != null ? type.getLibelle() : request.getRole());
        nouvellesValeurs.put("typePersonne", type != null ? type.getLibelle() : null);

        personne.setNom(request.getNom());
        personne.setContact(request.getContact());
        personne.setAdresse(request.getAdresse());
        personne.setEmail(request.getEmail());
        personne.setDate(LocalDateTime.now());
        personne.setRole(type != null ? type.getLibelle() : request.getRole());
        personne.setTypePersonne(type);

        PersonneDTO result = toDTO(personneRepository.save(personne));

        // ── Enregistrer l'audit ──
        auditService.enregistrer(
                "personne",
                String.valueOf(id),
                "MODIFICATION",
                anciennesValeurs,
                nouvellesValeurs
        );

        return result;
    }

    @Transactional
    public void supprimer(Integer id) {
        personneRepository.deleteById(id);
    }

    // ── Recherches par critère ──

    @Transactional(readOnly = true)
    public List<PersonneDTO> listerParRole(String role) {
        return personneRepository.findByRole(role).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PersonneDTO> rechercherParNom(String nom) {
        return personneRepository.findByNomContainingIgnoreCase(nom).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PersonneDTO> rechercherParDateRange(LocalDateTime dateDebut, LocalDateTime dateFin, Pageable pageable) {
        return personneRepository.searchByDateRange(dateDebut, dateFin, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<PersonneDTO> rechercherAvecFiltres(String search, LocalDateTime dateDebut, LocalDateTime dateFin, Pageable pageable) {
        return personneRepository.searchWithFilters(search, dateDebut, dateFin, pageable).map(this::toDTO);
    }

    // ── Helpers ──

    private TypePersonne trouverType(Integer id) {
        if (id == null) return null;
        return typePersonneRepository.findById(id).orElse(null);
    }

    private PersonneDTO toDTO(Personne p) {
        TypePersonne type = p.getTypePersonne();
        return PersonneDTO.builder()
                .idPersonne(p.getIdPersonne())
                .nom(p.getNom())
                .contact(p.getContact())
                .adresse(p.getAdresse())
                .email(p.getEmail())
                .date(p.getDate())
                .role(p.getRole())
                .idTypePersonne(type != null ? type.getIdTypePersonne() : null)
                .codeTypePersonne(type != null ? type.getCode() : null)
                .libelleTypePersonne(type != null ? type.getLibelle() : null)
                .build();
    }
}
