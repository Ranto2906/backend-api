package com.seimad.patrimoine.entity.dossier;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dossier_fournie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DossierFournie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dossier_fournie")
    private Integer idDossierFournie;

    @Column(name = "libelle", nullable = false, unique = true, length = 100)
    private String libelle;
}