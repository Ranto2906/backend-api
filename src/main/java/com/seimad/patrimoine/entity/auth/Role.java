package com.seimad.patrimoine.entity.auth;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_role")
    private Integer idRole;

    @Column(name = "nom_role", nullable = false, unique = true, length = 50)
    private String nomRole;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
