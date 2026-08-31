package com.seimad.patrimoine.entity.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_utilisateur")
    private Integer idUtilisateur;

    @Column(name = "nom_utilisateur", nullable = false, unique = true, length = 100)
    private String nomUtilisateur;

    @Column(name = "nom_complet", length = 200)
    private String nomComplet;

    @Column(name = "email", unique = true, length = 150)
    private String email;

    @Column(name = "mot_de_passe_hash", nullable = false)
    private String motDePasseHash;

    @Column(name = "actif")
    @Builder.Default
    private Boolean actif = true;

    @Column(name = "statut_compte", length = 20)
    @Builder.Default
    private String statutCompte = "en_attente_activation";

    @Column(name = "token_activation")
    private String tokenActivation;

    @Column(name = "date_expiration_activation")
    private LocalDateTime dateExpirationActivation;

    @Column(name = "tentatives_echouees")
    @Builder.Default
    private Integer tentativesEchouees = 0;

    @Column(name = "verrouille_jusqu_a")
    private LocalDateTime verrouilleJusquA;

    @Column(name = "date_creation")
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "derniere_connexion")
    private LocalDateTime derniereConnexion;

    // ── Relations RBAC (via table utilisateur_role) ──
    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UtilisateurRole> utilisateurRoles = new HashSet<>();
}
