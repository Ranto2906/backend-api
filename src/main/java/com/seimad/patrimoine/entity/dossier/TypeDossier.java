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

    @Column(name = "libelle", nullable = false, unique = true, length = 100)
    private String libelle;
}