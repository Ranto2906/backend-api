package com.seimad.patrimoine.dto.dossier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditDTO {
    private Integer idAudit;
    private String entiteType;
    private String entiteId;
    private String action;
    private String anciennesValeurs;
    private String nouvellesValeurs;
    private String dateAction;
    private String ipAdresse;
    private Integer idUtilisateur;
    private String nomUtilisateur;
}
