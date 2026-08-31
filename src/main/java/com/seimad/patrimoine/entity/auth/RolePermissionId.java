package com.seimad.patrimoine.entity.auth;

import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RolePermissionId implements Serializable {

    private Integer idRole;
    private Integer idPermission;
}
