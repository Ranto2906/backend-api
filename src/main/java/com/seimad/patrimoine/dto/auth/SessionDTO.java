package com.seimad.patrimoine.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDTO {
    private Integer idSession;
    private String userAgent;
    private String ipAdresse;
    private String dateCreation;
    private String dateExpiration;
    private Boolean revoque;
    private Integer idUtilisateur;
    private String nomUtilisateur;
}
