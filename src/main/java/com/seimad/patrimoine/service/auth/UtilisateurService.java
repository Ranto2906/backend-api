package com.seimad.patrimoine.service.auth;

import com.seimad.patrimoine.dto.auth.*;
import com.seimad.patrimoine.entity.auth.Role;
import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.auth.UtilisateurRole;
import com.seimad.patrimoine.repository.auth.RoleRepository;
import com.seimad.patrimoine.repository.auth.UtilisateurRepository;
import com.seimad.patrimoine.repository.auth.UtilisateurRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final UtilisateurRoleRepository utilisateurRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<UtilisateurDTO> lister(String search, Pageable pageable) {
        return utilisateurRepository.search(search, pageable)
                .map(this::toDTO);
    }

    public UtilisateurDTO trouverParId(Integer id) {
        Utilisateur u = utilisateurRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur non trouvé avec l'id : " + id));
        return toDTO(u);
    }

    @Transactional
    public UtilisateurDTO creer(CreateUtilisateurRequest request) {
        if (utilisateurRepository.existsByNomUtilisateur(request.getNomUtilisateur())) {
            throw new IllegalArgumentException("Le nom d'utilisateur '" + request.getNomUtilisateur() + "' est déjà utilisé");
        }
        if (request.getEmail() != null && utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("L'email '" + request.getEmail() + "' est déjà utilisé");
        }
        Utilisateur utilisateur = Utilisateur.builder()
                .nomUtilisateur(request.getNomUtilisateur())
                .nomComplet(request.getNomComplet())
                .email(request.getEmail())
                .motDePasseHash(passwordEncoder.encode(request.getMotDePasse()))
                .actif(request.getActif() != null ? request.getActif() : true)
                .statutCompte("actif")
                .build();
        utilisateur = utilisateurRepository.save(utilisateur);
        log.info("Utilisateur créé : {} (id={})", utilisateur.getNomUtilisateur(), utilisateur.getIdUtilisateur());
        return toDTO(utilisateur);
    }

    @Transactional
    public UtilisateurDTO mettreAJour(Integer id, UpdateUtilisateurRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur non trouvé avec l'id : " + id));
        if (request.getNomUtilisateur() != null) {
            if (!request.getNomUtilisateur().equals(utilisateur.getNomUtilisateur())
                    && utilisateurRepository.existsByNomUtilisateur(request.getNomUtilisateur())) {
                throw new IllegalArgumentException("Le nom d'utilisateur '" + request.getNomUtilisateur() + "' est déjà utilisé");
            }
            utilisateur.setNomUtilisateur(request.getNomUtilisateur());
        }
        if (request.getNomComplet() != null) {
            utilisateur.setNomComplet(request.getNomComplet());
        }
        if (request.getEmail() != null) {
            if (!request.getEmail().equals(utilisateur.getEmail())
                    && utilisateurRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("L'email '" + request.getEmail() + "' est déjà utilisé");
            }
            utilisateur.setEmail(request.getEmail());
        }
        if (request.getMotDePasse() != null && !request.getMotDePasse().isEmpty()) {
            utilisateur.setMotDePasseHash(passwordEncoder.encode(request.getMotDePasse()));
        }
        if (request.getActif() != null) {
            utilisateur.setActif(request.getActif());
            if (Boolean.FALSE.equals(request.getActif())) {
                utilisateur.setStatutCompte("desactive");
            }
        }
        if (request.getStatutCompte() != null) {
            utilisateur.setStatutCompte(request.getStatutCompte());
        }
        utilisateur = utilisateurRepository.save(utilisateur);
        log.info("Utilisateur mis à jour : {} (id={})", utilisateur.getNomUtilisateur(), id);
        return toDTO(utilisateur);
    }

    @Transactional
    public UtilisateurDTO activerDesactiver(Integer id, boolean actif) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur non trouvé avec l'id : " + id));
        utilisateur.setActif(actif);
        utilisateur.setStatutCompte(actif ? "actif" : "desactive");
        utilisateur = utilisateurRepository.save(utilisateur);
        log.info("Utilisateur {} : actif={}", utilisateur.getNomUtilisateur(), actif);
        return toDTO(utilisateur);
    }

    @Transactional
    public void reinitialiserMotDePasse(Integer id, String nouveauMotDePasse) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur non trouvé avec l'id : " + id));
        utilisateur.setMotDePasseHash(passwordEncoder.encode(nouveauMotDePasse));
        utilisateur.setTentativesEchouees(0);
        utilisateur.setVerrouilleJusquA(null);
        utilisateurRepository.save(utilisateur);
        log.info("Mot de passe réinitialisé pour {} (id={})", utilisateur.getNomUtilisateur(), id);
    }

    @Transactional
    public void changerMotDePasse(Integer id, ChangePasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur non trouvé avec l'id : " + id));
        if (!passwordEncoder.matches(request.getAncienMotDePasse(), utilisateur.getMotDePasseHash())) {
            throw new IllegalArgumentException("L'ancien mot de passe est incorrect");
        }
        utilisateur.setMotDePasseHash(passwordEncoder.encode(request.getNouveauMotDePasse()));
        utilisateurRepository.save(utilisateur);
        log.info("Mot de passe changé pour {} (id={})", utilisateur.getNomUtilisateur(), id);
    }

    @Transactional
    public void supprimer(Integer id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur non trouvé avec l'id : " + id));
        utilisateurRoleRepository.deleteByUtilisateurIdUtilisateurAndRoleIdRole(id, null);
        utilisateurRepository.delete(utilisateur);
        log.info("Utilisateur supprimé : {} (id={})", utilisateur.getNomUtilisateur(), id);
    }

    @Transactional
    public void attribuerRole(Integer idUtilisateur, Integer idRole) {
        Utilisateur utilisateur = utilisateurRepository.findById(idUtilisateur)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur non trouvé : " + idUtilisateur));
        Role role = roleRepository.findById(idRole)
                .orElseThrow(() -> new NoSuchElementException("Rôle non trouvé : " + idRole));
        if (utilisateurRoleRepository.existsByUtilisateurIdUtilisateurAndRoleIdRole(idUtilisateur, idRole)) {
            throw new IllegalArgumentException("Le rôle '" + role.getNomRole() + "' est déjà attribué à '" + utilisateur.getNomUtilisateur() + "'");
        }
        utilisateurRoleRepository.save(UtilisateurRole.builder()
                .idUtilisateur(idUtilisateur)
                .idRole(idRole)
                .utilisateur(utilisateur)
                .role(role)
                .build());
        log.info("Rôle '{}' → utilisateur '{}'", role.getNomRole(), utilisateur.getNomUtilisateur());
    }

    @Transactional
    public void retirerRole(Integer idUtilisateur, Integer idRole) {
        utilisateurRoleRepository.deleteByUtilisateurIdUtilisateurAndRoleIdRole(idUtilisateur, idRole);
        log.info("Rôle {} retiré de l'utilisateur {}", idRole, idUtilisateur);
    }

    public List<RoleDTO> listerRoles(Integer idUtilisateur) {
        return utilisateurRoleRepository.findByUtilisateurIdUtilisateur(idUtilisateur)
                .stream()
                .map(ur -> RoleDTO.builder()
                        .idRole(ur.getRole().getIdRole())
                        .nomRole(ur.getRole().getNomRole())
                        .description(ur.getRole().getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    private UtilisateurDTO toDTO(Utilisateur u) {
        List<RoleDTO> rolesRBAC = utilisateurRoleRepository
                .findByUtilisateurIdUtilisateur(u.getIdUtilisateur())
                .stream()
                .map(ur -> RoleDTO.builder()
                        .idRole(ur.getRole().getIdRole())
                        .nomRole(ur.getRole().getNomRole())
                        .description(ur.getRole().getDescription())
                        .build())
                .collect(Collectors.toList());
        return UtilisateurDTO.builder()
                .idUtilisateur(u.getIdUtilisateur())
                .nomUtilisateur(u.getNomUtilisateur())
                .nomComplet(u.getNomComplet())
                .email(u.getEmail())
                .actif(u.getActif())
                .statutCompte(u.getStatutCompte())
                .tentativesEchouees(u.getTentativesEchouees())
                .dateCreation(u.getDateCreation())
                .derniereConnexion(u.getDerniereConnexion())
                .roles(rolesRBAC)
                .build();
    }
}
