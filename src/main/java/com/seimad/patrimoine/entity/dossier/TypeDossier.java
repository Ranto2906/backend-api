package com.seimad.patrimoine.entity.dossier;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "type_dossier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeDossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_type_dossier")
    private Integer idTypeDossier;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;
}