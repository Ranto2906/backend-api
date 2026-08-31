package com.seimad.patrimoine.controller.auth;

import com.seimad.patrimoine.dto.auth.*;
import com.seimad.patrimoine.service.auth.AuthService;
import com.seimad.patrimoine.service.auth.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller d'authentification (RG-01 à RG-06).
 * Endpoints : login, register, refresh, logout, me.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentification", description = "Connexion, inscription, refresh token, déconnexion")
public class AuthController {

    private final AuthService authService;
    private final UtilisateurService utilisateurService;

    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur", description = "Authentifie l'utilisateur et retourne les tokens JWT")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Inscription utilisateur", description = "Crée un nouveau compte utilisateur")
    public ResponseEntity<UtilisateurDTO> register(
            @Valid @RequestBody CreateUtilisateurRequest request) {
        UtilisateurDTO utilisateur = utilisateurService.creer(request);
        return ResponseEntity.ok(utilisateur);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir le token", description = "Retourne un nouvel access token à partir du refresh token")
    public ResponseEntity<LoginResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion", description = "Révoque le refresh token de l'utilisateur")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) TokenRefreshRequest request) {
        String refreshToken = request != null ? request.getRefreshToken() : null;
        authService.logout(refreshToken);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Utilisateur connecté", description = "Retourne les informations de l'utilisateur courant")
    public ResponseEntity<UtilisateurDTO> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).build();
        }

        String nomUtilisateur = auth.getName();
        var users = utilisateurService.lister(nomUtilisateur,
                org.springframework.data.domain.PageRequest.of(0, 1));
        if (users.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(users.getContent().get(0));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Changer le mot de passe", description = "Change le mot de passe de l'utilisateur connecté")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String nomUtilisateur = auth.getName();

        var users = utilisateurService.lister(nomUtilisateur,
                org.springframework.data.domain.PageRequest.of(0, 1));
        if (users.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Integer userId = users.getContent().get(0).getIdUtilisateur();
        utilisateurService.changerMotDePasse(userId, request);
        return ResponseEntity.ok().build();
    }
}
