package com.seimad.patrimoine.entity.notification;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "type_personne")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypePersonne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_type_personne")
    private Integer idTypePersonne;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "libelle", nullable = false, length = 100)
    private String libelle;
}