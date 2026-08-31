package com.seimad.patrimoine.entity.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "utilisateur_role")
@IdClass(UtilisateurRoleId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilisateurRole {

    @Id
    @Column(name = "id_utilisateur")
    private Integer idUtilisateur;

    @Id
    @Column(name = "id_role")
    private Integer idRole;

    @Column(name = "date_attribution")
    @Builder.Default
    private LocalDateTime dateAttribution = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", insertable = false, updatable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_role", insertable = false, updatable = false)
    private Role role;
}
