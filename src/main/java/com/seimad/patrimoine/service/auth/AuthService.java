package com.seimad.patrimoine.service.auth;

import com.seimad.patrimoine.dto.auth.*;
import com.seimad.patrimoine.entity.auth.JournalConnexion;
import com.seimad.patrimoine.entity.auth.SessionUtilisateur;
import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.repository.auth.JournalConnexionRepository;
import com.seimad.patrimoine.repository.auth.SessionUtilisateurRepository;
import com.seimad.patrimoine.repository.auth.UtilisateurRepository;
import com.seimad.patrimoine.security.CustomUserDetailsService;
import com.seimad.patrimoine.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UtilisateurRepository utilisateurRepository;
    private final JournalConnexionRepository journalConnexionRepository;
    private final SessionUtilisateurRepository sessionUtilisateurRepository;
    private final CustomUserDetailsService userDetailsService;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Utilisateur utilisateur = utilisateurRepository
                .findByNomUtilisateur(request.getNomUtilisateur())
                .orElseThrow(() -> {
                    enregistrerConnexion(null, false, httpRequest);
                    return new BadCredentialsException("Identifiant ou mot de passe incorrect");
                });

        if (Boolean.FALSE.equals(utilisateur.getActif())
                || "desactive".equals(utilisateur.getStatutCompte())) {
            enregistrerConnexion(utilisateur, false, httpRequest);
            throw new DisabledException("Compte désactivé. Contactez l'administrateur.");
        }

        if (utilisateur.getVerrouilleJusquA() != null
                && utilisateur.getVerrouilleJusquA().isAfter(LocalDateTime.now())) {
            enregistrerConnexion(utilisateur, false, httpRequest);
            throw new LockedException("Compte verrouillé. Réessayez après " + utilisateur.getVerrouilleJusquA());
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getNomUtilisateur(), request.getMotDePasse())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            utilisateur.setTentativesEchouees(0);
            utilisateur.setVerrouilleJusquA(null);
            utilisateur.setDerniereConnexion(LocalDateTime.now());
            utilisateur.setStatutCompte("actif");
            utilisateurRepository.save(utilisateur);

            enregistrerConnexion(utilisateur, true, httpRequest);

            var userDetails = userDetailsService.loadUserById(utilisateur.getIdUtilisateur());
            java.util.List<String> roles = userDetailsService.getUserRoleNames(utilisateur);

            String accessToken = tokenProvider.generateAccessToken(utilisateur, roles);
            String refreshToken = tokenProvider.generateRefreshToken(utilisateur);

            SessionUtilisateur session = SessionUtilisateur.builder()
                    .tokenRafraichissement(refreshToken)
                    .ipAdresse(getClientIp(httpRequest))
                    .userAgent(httpRequest.getHeader("User-Agent"))
                    .dateExpiration(LocalDateTime.now().plusSeconds(tokenProvider.getRefreshTokenExpirationMs() / 1000))
                    .utilisateur(utilisateur)
                    .build();
            sessionUtilisateurRepository.save(session);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(tokenProvider.getAccessTokenExpirationMs() / 1000)
                    .utilisateur(toDTO(utilisateur, roles))
                    .build();

        } catch (BadCredentialsException e) {
            incrementerTentatives(utilisateur);
            enregistrerConnexion(utilisateur, false, httpRequest);
            throw new BadCredentialsException("Identifiant ou mot de passe incorrect");
        }
    }

    @Transactional
    public LoginResponse refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!tokenProvider.validateToken(refreshToken))
            throw new BadCredentialsException("Refresh token invalide ou expiré");
        if (!tokenProvider.isRefreshToken(refreshToken))
            throw new BadCredentialsException("Le token n'est pas un refresh token");

        SessionUtilisateur session = sessionUtilisateurRepository
                .findByTokenRafraichissementAndRevoqueFalse(refreshToken)
                .orElseThrow(() -> new BadCredentialsException("Session révoquée."));

        Integer userId = tokenProvider.getUserIdFromToken(refreshToken);
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Utilisateur introuvable"));

        var roles = userDetailsService.getUserRoleNames(utilisateur);
        String newAccessToken = tokenProvider.generateAccessToken(utilisateur, roles);
        String newRefreshToken = tokenProvider.generateRefreshToken(utilisateur);

        session.setRevoque(true);
        sessionUtilisateurRepository.save(session);

        SessionUtilisateur newSession = SessionUtilisateur.builder()
                .tokenRafraichissement(newRefreshToken)
                .userAgent(session.getUserAgent())
                .ipAdresse(session.getIpAdresse())
                .dateExpiration(LocalDateTime.now().plusSeconds(tokenProvider.getRefreshTokenExpirationMs() / 1000))
                .utilisateur(utilisateur)
                .build();
        sessionUtilisateurRepository.save(newSession);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpirationMs() / 1000)
                .utilisateur(toDTO(utilisateur, roles))
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            sessionUtilisateurRepository.findByTokenRafraichissementAndRevoqueFalse(refreshToken)
                    .ifPresent(session -> {
                        session.setRevoque(true);
                        sessionUtilisateurRepository.save(session);
                    });
        }
        SecurityContextHolder.clearContext();
    }

    private void incrementerTentatives(Utilisateur utilisateur) {
        int tentatives = utilisateur.getTentativesEchouees() + 1;
        utilisateur.setTentativesEchouees(tentatives);
        if (tentatives >= MAX_FAILED_ATTEMPTS) {
            utilisateur.setVerrouilleJusquA(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
            log.warn("Utilisateur {} verrouillé après {} tentatives échouées", utilisateur.getNomUtilisateur(), tentatives);
        }
        utilisateurRepository.save(utilisateur);
    }

    private void enregistrerConnexion(Utilisateur utilisateur, boolean succes, HttpServletRequest request) {
        JournalConnexion journal = JournalConnexion.builder()
                .succes(succes)
                .ipAdresse(getClientIp(request))
                .utilisateur(utilisateur)
                .build();
        journalConnexionRepository.save(journal);
    }

    private InetAddress getClientIp(HttpServletRequest request) {
        try {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty())
                return InetAddress.getByName(xForwardedFor.split(",")[0].trim());
            return InetAddress.getByName(request.getRemoteAddr());
        } catch (UnknownHostException e) {
            log.warn("Impossible de résoudre l'IP du client : {}", e.getMessage());
            return null;
        }
    }

    private UtilisateurDTO toDTO(Utilisateur u, java.util.List<String> roles) {
        return UtilisateurDTO.builder()
                .idUtilisateur(u.getIdUtilisateur())
                .nomUtilisateur(u.getNomUtilisateur())
                .nomComplet(u.getNomComplet())
                .email(u.getEmail())
                .actif(u.getActif())
                .statutCompte(u.getStatutCompte())
                .dateCreation(u.getDateCreation())
                .derniereConnexion(u.getDerniereConnexion())
                .roles(roles.stream().map(r -> RoleDTO.builder().nomRole(r).build()).toList())
                .build();
    }
}
