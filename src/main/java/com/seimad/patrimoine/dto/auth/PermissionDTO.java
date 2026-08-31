package com.seimad.patrimoine.dto.auth;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionDTO {

    private Integer idPermission;
    private String codeModule;
    private String libelleModule;
    private String codeEntite;
    private String libelleEntite;
    private String codeAction;
}
