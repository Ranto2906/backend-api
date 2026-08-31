package com.seimad.patrimoine.entity.dossier;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dossier_demandeur")
@IdClass(DossierDemandeurId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DossierDemandeur {

    @Id
    @Column(name = "id_dossier")
    private Integer idDossier;

    @Id
    @Column(name = "id_demandeur")
    private Integer idDemandeur;

    @Column(name = "role", length = 30)
    @Builder.Default
    private String role = "Principal";

    @Column(name = "date_ajout")
    @Builder.Default
    private LocalDateTime dateAjout = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dossier", insertable = false, updatable = false)
    private Dossier dossier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_demandeur", insertable = false, updatable = false)
    private Demandeur demandeur;
}
