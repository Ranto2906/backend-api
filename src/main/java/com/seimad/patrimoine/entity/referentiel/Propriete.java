package com.seimad.patrimoine.entity.referentiel;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "propriete")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Propriete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_propriete")
    private Integer idPropriete;

    @Column(name = "nom", nullable = false, unique = true, length = 200)
    private String nom;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ville")
    private Ville ville;
}
