package com.seimad.patrimoine.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtapeNotificationDTO {
    private Integer idEtapeNotification;
    private String codeEtape;
    private String libelle;
    private Integer ordre;
    private Boolean estFinal;
    private LocalDateTime createdAt;
}
