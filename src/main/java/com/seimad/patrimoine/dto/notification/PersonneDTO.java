package com.seimad.patrimoine.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonneDTO {
    private Integer idPersonne;
    private String nom;
    private String contact;
    private String adresse;
    private String role;
}
