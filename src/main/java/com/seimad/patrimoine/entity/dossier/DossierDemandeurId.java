package com.seimad.patrimoine.entity.dossier;

import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DossierDemandeurId implements Serializable {

    private Integer idDossier;
    private Integer idPersonne;
}