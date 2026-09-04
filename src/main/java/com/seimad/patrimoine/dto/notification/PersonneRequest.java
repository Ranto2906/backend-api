package com.seimad.patrimoine.dto.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonneRequest {
    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    private String contact;
    private String adresse;
    private String role;
    private String email;
    private Integer idTypePersonne;
}
