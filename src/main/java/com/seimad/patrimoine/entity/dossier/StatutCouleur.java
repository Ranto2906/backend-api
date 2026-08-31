package com.seimad.patrimoine.entity.dossier;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "statut_couleur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatutCouleur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_statut_couleur")
    private Integer idStatutCouleur;

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "libelle", nullable = false, length = 30)
    private String libelle;

    @Column(name = "couleur_hex", nullable = false, length = 10)
    private String couleurHex;
}
