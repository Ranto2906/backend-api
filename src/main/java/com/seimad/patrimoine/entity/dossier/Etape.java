package com.seimad.patrimoine.entity.dossier;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "etape")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Etape {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_etape")
    private Integer idEtape;

    @Column(name = "code_etape", nullable = false, unique = true, length = 50)
    private String codeEtape;

    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;

    @Column(name = "ordre", nullable = false, unique = true)
    private Integer ordre;

    @Column(name = "duree_previsionnelle")
    private Integer dureePrevisionnelle;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
