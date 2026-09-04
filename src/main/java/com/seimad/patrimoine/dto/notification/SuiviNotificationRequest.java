package com.seimad.patrimoine.dto.notification;

import jakarta.validation.constraints.NotNull;
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
public class SuiviNotificationRequest {
    private UUID idNotification;
    private Integer ordre;
    private LocalDate dateSuivi;
    private String constats;
    private String actionsASuivre;
}
