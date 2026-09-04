package com.seimad.patrimoine.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonneDTO {
    private Integer idPersonne;
    private String nom;
    private String contact;
    private String adresse;
    private String email;
    private LocalDateTime date;
    private String role;
    // Type personne (référentiel)
    private Integer idTypePersonne;
    private String codeTypePersonne;
    private String libelleTypePersonne;
}
