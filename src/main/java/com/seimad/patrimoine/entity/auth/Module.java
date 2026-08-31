package com.seimad.patrimoine.entity.auth;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "module_")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_module")
    private Integer idModule;

    @Column(name = "code_module", nullable = false, unique = true, length = 50)
    private String codeModule;

    @Column(name = "libelle", nullable = false, length = 100)
    private String libelle;
}
