package com.seimad.patrimoine.dto.auth;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleWithPermissionsDTO {

    private Integer idRole;
    private String nomRole;
    private String description;
    private List<PermissionDTO> permissions;
}
