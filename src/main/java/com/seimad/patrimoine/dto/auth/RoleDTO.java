package com.seimad.patrimoine.dto.auth;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDTO {

    private Integer idRole;
    private String nomRole;
    private String description;
}
