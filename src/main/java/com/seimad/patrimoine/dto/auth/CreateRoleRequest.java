package com.seimad.patrimoine.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequest {

    @NotBlank(message = "Le nom du rôle est requis")
    private String nomRole;

    private String description;
}
