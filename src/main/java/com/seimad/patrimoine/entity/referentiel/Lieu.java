package com.seimad.patrimoine.entity.referentiel;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lieu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lieu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lieu")
    private Integer idLieu;

    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ville", nullable = false)
    private Ville ville;
}