package com.seimad.patrimoine.dto.dossier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Document joint à une personne (demandeur) : photo ou pièce
 * (table document_demandeur — « document-personne »).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDemandeurDTO {

    private Integer idDocument;
    private String nomFichier;
    private String typeDocument;
    private Long tailleOctets;
    private LocalDateTime dateUpload;
    private Integer idPersonne;
    private String nomPersonne;
}
