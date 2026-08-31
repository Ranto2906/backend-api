package com.seimad.patrimoine.entity.auth;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "entite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entite")
    private Integer idEntite;

    @Column(name = "code_entite", nullable = false, unique = true, length = 50)
    private String codeEntite;

    @Column(name = "libelle", nullable = false, length = 100)
    private String libelle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_module", nullable = false)
    private Module module;
}
