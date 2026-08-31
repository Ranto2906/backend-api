package com.seimad.patrimoine.dto.auth;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    private String ancienMotDePasse;
    private String nouveauMotDePasse;
}
