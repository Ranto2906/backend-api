package com.seimad.patrimoine.security;

import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.auth.UtilisateurRole;
import com.seimad.patrimoine.repository.auth.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String nomUtilisateur) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByNomUtilisateur(nomUtilisateur)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouve : " + nomUtilisateur));
        return buildUserDetails(utilisateur);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Integer idUtilisateur) {
        Utilisateur utilisateur = utilisateurRepository.findById(idUtilisateur)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouve : id=" + idUtilisateur));
        return buildUserDetails(utilisateur);
    }

    private UserDetails buildUserDetails(Utilisateur utilisateur) {
        Set<String> roleNames = new HashSet<>();

        if (utilisateur.getUtilisateurRoles() != null) {
            for (UtilisateurRole ur : utilisateur.getUtilisateurRoles()) {
                if (ur.getRole() != null) {
                    roleNames.add(ur.getRole().getNomRole());
                }
            }
        }

        if (roleNames.isEmpty()) {
            roleNames.add("USER");
        }

        List<SimpleGrantedAuthority> authorities = roleNames.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        log.debug("Utilisateur {} -> roles : {}", utilisateur.getNomUtilisateur(), roleNames);

        boolean enabled = Boolean.TRUE.equals(utilisateur.getActif());

        return new User(
                utilisateur.getNomUtilisateur(),
                utilisateur.getMotDePasseHash(),
                enabled,
                true,
                true,
                enabled,
                authorities
        );
    }

    public List<String> getUserRoleNames(Utilisateur utilisateur) {
        Set<String> roleNames = new HashSet<>();

        if (utilisateur.getUtilisateurRoles() != null) {
            for (UtilisateurRole ur : utilisateur.getUtilisateurRoles()) {
                if (ur.getRole() != null) {
                    roleNames.add(ur.getRole().getNomRole());
                }
            }
        }

        if (roleNames.isEmpty()) {
            roleNames.add("USER");
        }

        return new ArrayList<>(roleNames);
    }
}
