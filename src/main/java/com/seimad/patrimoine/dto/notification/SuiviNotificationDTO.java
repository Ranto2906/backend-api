package com.seimad.patrimoine.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuiviNotificationDTO {
    private UUID idSuivi;
    private Integer ordre;
    private LocalDate dateSuivi;
    private String constats;
    private String actionsASuivre;
    private UUID idNotification;
}
