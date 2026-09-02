package com.seimad.patrimoine.dto.notification;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPersonneRequest {
    @NotNull(message = "L'identifiant de la personne est obligatoire")
    private Integer idPersonne;
    private String roleDansNotification;
}
