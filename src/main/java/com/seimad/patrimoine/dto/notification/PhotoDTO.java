package com.seimad.patrimoine.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoDTO {
    private UUID idPhoto;
    private String entiteType;
    private UUID entiteId;
    private String typePhoto;
    private String observation;
    private LocalDate datePrise;
    private LocalDateTime dateCreation;
}
