package com.seimad.patrimoine.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUtilisateurRequest {

    @Size(min = 3, max = 100, message = "Le nom d'utilisateur doit contenir entre 3 et 100 caractères")
    private String nomUtilisateur;

    @Size(max = 200, message = "Le nom complet ne peut pas dépasser 200 caractères")
    private String nomComplet;

    @Email(message = "L'adresse email n'est pas valide")
    private String email;

    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String motDePasse;

    private Boolean actif;

    private String statutCompte;
}
