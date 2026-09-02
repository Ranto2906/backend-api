package com.seimad.patrimoine.service.notification;

import com.seimad.patrimoine.dto.notification.PersonneDTO;
import com.seimad.patrimoine.dto.notification.PersonneRequest;
import com.seimad.patrimoine.entity.notification.Personne;
import com.seimad.patrimoine.repository.notification.PersonneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PersonneService {

    private final PersonneRepository personneRepository;

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
        Personne personne = Personne.builder()
                .nom(request.getNom())
                .contact(request.getContact())
                .adresse(request.getAdresse())
                .role(request.getRole())
                .build();
        return toDTO(personneRepository.save(personne));
    }

    @Transactional
    public PersonneDTO mettreAJour(Integer id, PersonneRequest request) {
        Personne personne = personneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Personne non trouvée avec l'id : " + id));
        personne.setNom(request.getNom());
        personne.setContact(request.getContact());
        personne.setAdresse(request.getAdresse());
        personne.setRole(request.getRole());
        return toDTO(personneRepository.save(personne));
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

    // ── Helpers ──

    private PersonneDTO toDTO(Personne p) {
        return PersonneDTO.builder()
                .idPersonne(p.getIdPersonne())
                .nom(p.getNom())
                .contact(p.getContact())
                .adresse(p.getAdresse())
                .role(p.getRole())
                .build();
    }
}
