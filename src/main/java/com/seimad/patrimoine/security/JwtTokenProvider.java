package com.seimad.patrimoine.security;

import com.seimad.patrimoine.entity.auth.Utilisateur;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de génération et validation des tokens JWT (access + refresh).
 * Conforme RG-01, RG-06 : session via JWT access + refresh ; révocation possible.
 */
@Service
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret:Y2hhdmUtbGEtc2VjdXJpdGUta2V5LWJhc2U2NC12YWx1ZS1wb3VyLWpld3Qtc2VjcmV0LWtleQ==}")
    private String secretBase64;

    @Value("${jwt.access-token-expiration-ms:900000}") // 15 min
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms:604800000}") // 7 jours
    private long refreshTokenExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
    }

    // ──────────────────────────────────────────────
    // Génération de tokens
    // ──────────────────────────────────────────────

    /**
     * Génère un access token JWT pour l'utilisateur donné.
     * Le subject = idUtilisateur, claims = nomUtilisateur + roles.
     */
    public String generateAccessToken(Utilisateur utilisateur, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(utilisateur.getIdUtilisateur()))
                .claim("nomUtilisateur", utilisateur.getNomUtilisateur())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Génère un refresh token JWT (durée de vie longue, 7 jours).
     */
    public String generateRefreshToken(Utilisateur utilisateur) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(utilisateur.getIdUtilisateur()))
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .id(UUID.randomUUID().toString()) // unique ID pour révocation
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // ──────────────────────────────────────────────
    // Extraction des données
    // ──────────────────────────────────────────────

    public Integer getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Integer.parseInt(claims.getSubject());
    }

    public String getNomUtilisateurFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("nomUtilisateur", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = parseToken(token);
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Retourne les granted authorities Spring Security à partir des rôles du token.
     */
    public Collection<? extends SimpleGrantedAuthority> getAuthoritiesFromToken(String token) {
        return getRolesFromToken(token).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }

    public boolean isRefreshToken(String token) {
        Claims claims = parseToken(token);
        return "refresh".equals(claims.get("type", String.class));
    }

    // ──────────────────────────────────────────────
    // Validation
    // ──────────────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expiré : {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Token JWT malformé : {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Token JWT non supporté : {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Token JWT vide : {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("Signature JWT invalide : {}", e.getMessage());
        }
        return false;
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    // ──────────────────────────────────────────────
    // Expiration config
    // ──────────────────────────────────────────────

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    // ──────────────────────────────────────────────
    // Interne
    // ──────────────────────────────────────────────

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
