package com.seimad.patrimoine.entity.signalement;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "type_signalement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeSignalement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_type_signalement")
    private Integer idTypeSignalement;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "libelle", nullable = false, length = 100)
    private String libelle;

    @Column(name = "couleur", length = 10)
    @Builder.Default
    private String couleur = "#a13a2c";
}
