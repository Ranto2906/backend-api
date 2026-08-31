package com.seimad.patrimoine.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Le nom d'utilisateur est requis")
    private String nomUtilisateur;

    @NotBlank(message = "Le mot de passe est requis")
    private String motDePasse;
}
