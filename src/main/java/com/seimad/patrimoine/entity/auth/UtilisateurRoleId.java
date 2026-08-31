package com.seimad.patrimoine.entity.auth;

import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UtilisateurRoleId implements Serializable {

    private Integer idUtilisateur;
    private Integer idRole;
}
