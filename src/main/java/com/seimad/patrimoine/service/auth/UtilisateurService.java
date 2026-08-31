package com.seimad.patrimoine.service.auth;

import com.seimad.patrimoine.dto.auth.*;
import com.seimad.patrimoine.entity.auth.Role;
import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.auth.UtilisateurRole;
import com.seimad.patrimoine.repository.auth.RoleRepository;
import com.seimad.patrimoine.repository.auth.UtilisateurRepository;
import com.seimad.patrimoine.repository.auth.UtilisateurRoleRepository;
import com.seimad.patrimoine.service.dossier.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    private final AuditService auditService;

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

        // ── Audit ──
        auditService.enregistrer("utilisateur",
                String.valueOf(utilisateur.getIdUtilisateur()), "CREATE",
                null, toMap(utilisateur), getConnectedUser(), getRequest());

        return toDTO(utilisateur);
    }

    @Transactional
    public UtilisateurDTO mettreAJour(Integer id, UpdateUtilisateurRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur non trouvé avec l'id : " + id));

        // Valeurs avant modification
        Map<String, Object> avant = toMap(utilisateur);

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

        // ── Audit ──
        auditService.enregistrer("utilisateur", String.valueOf(id), "UPDATE",
                avant, toMap(utilisateur), getConnectedUser(), getRequest());

        return toDTO(utilisateur);
    }

    @Transactional
    public UtilisateurDTO activerDesactiver(Integer id, boolean actif) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur non trouvé avec l'id : " + id));

        Map<String, Object> avant = toMap(utilisateur);

        utilisateur.setActif(actif);
        utilisateur.setStatutCompte(actif ? "actif" : "desactive");
        utilisateur = utilisateurRepository.save(utilisateur);
        log.info("Utilisateur {} : actif={}", utilisateur.getNomUtilisateur(), actif);

        // ── Audit ──
        auditService.enregistrer("utilisateur", String.valueOf(id), "UPDATE",
                avant, toMap(utilisateur), getConnectedUser(), getRequest());

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

        // ── Audit ──
        auditService.enregistrer("utilisateur", String.valueOf(id), "UPDATE",
                null, Map.of("action", "reset_password"), getConnectedUser(), getRequest());
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

        // ── Audit ──
        auditService.enregistrer("utilisateur", String.valueOf(id), "UPDATE",
                null, Map.of("action", "change_password"), getConnectedUser(), getRequest());
    }

    @Transactional
    public void supprimer(Integer id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur non trouvé avec l'id : " + id));

        Map<String, Object> avant = toMap(utilisateur);

        utilisateurRoleRepository.deleteByUtilisateurIdUtilisateurAndRoleIdRole(id, null);
        utilisateurRepository.delete(utilisateur);
        log.info("Utilisateur supprimé : {} (id={})", utilisateur.getNomUtilisateur(), id);

        // ── Audit ──
        auditService.enregistrer("utilisateur", String.valueOf(id), "DELETE",
                avant, null, getConnectedUser(), getRequest());
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

        // ── Audit ──
        auditService.enregistrer("utilisateur", String.valueOf(idUtilisateur), "UPDATE",
                null, Map.of("action", "assign_role", "roleId", idRole, "roleNom", role.getNomRole()),
                getConnectedUser(), getRequest());
    }

    @Transactional
    public void retirerRole(Integer idUtilisateur, Integer idRole) {
        utilisateurRoleRepository.deleteByUtilisateurIdUtilisateurAndRoleIdRole(idUtilisateur, idRole);
        log.info("Rôle {} retiré de l'utilisateur {}", idRole, idUtilisateur);

        // ── Audit ──
        auditService.enregistrer("utilisateur", String.valueOf(idUtilisateur), "UPDATE",
                null, Map.of("action", "remove_role", "roleId", idRole),
                getConnectedUser(), getRequest());
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

    // ── Helpers ──

    private Map<String, Object> toMap(Utilisateur u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("idUtilisateur", u.getIdUtilisateur());
        map.put("nomUtilisateur", u.getNomUtilisateur());
        map.put("nomComplet", u.getNomComplet());
        map.put("email", u.getEmail());
        map.put("actif", u.getActif());
        map.put("statutCompte", u.getStatutCompte());
        return map;
    }

    private Utilisateur getConnectedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getPrincipal())) return null;
        String nomUtilisateur = auth.getName();
        return utilisateurRepository.findByNomUtilisateur(nomUtilisateur).orElse(null);
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
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
