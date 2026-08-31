package com.seimad.patrimoine.entity.dossier;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "statut_dossier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatutDossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_statut_dossier")
    private Integer idStatutDossier;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "libelle", nullable = false, length = 50)
    private String libelle;

    @Column(name = "couleur_hex", length = 10)
    @Builder.Default
    private String couleurHex = "#8a8578";

    @Column(name = "est_final")
    @Builder.Default
    private Boolean estFinal = false;

    @Column(name = "ordre")
    private Integer ordre;
}
