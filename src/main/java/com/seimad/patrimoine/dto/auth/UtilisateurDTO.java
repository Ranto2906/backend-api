package com.seimad.patrimoine.dto.auth;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilisateurDTO {

    private Integer idUtilisateur;
    private String nomUtilisateur;
    private String nomComplet;
    private String email;
    private Boolean actif;
    private String statutCompte;
    private Integer tentativesEchouees;
    private LocalDateTime dateCreation;
    private LocalDateTime derniereConnexion;
    private List<RoleDTO> roles;    // rôles RBAC (utilisateur_role)
}
