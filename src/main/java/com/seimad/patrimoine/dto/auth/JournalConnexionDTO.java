package com.seimad.patrimoine.dto.auth;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalConnexionDTO {

    private Integer idJournal;
    private String dateConnexion;
    private String ipAdresse;
    private Boolean succes;
    private Integer idUtilisateur;
    private String nomUtilisateur;
}
