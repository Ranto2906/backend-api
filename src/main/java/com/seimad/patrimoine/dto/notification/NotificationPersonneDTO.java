package com.seimad.patrimoine.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPersonneDTO {
    private UUID idNotification;
    private Integer idPersonne;
    private String nomPersonne;
    private String roleDansNotification;
    private String contact;
}
