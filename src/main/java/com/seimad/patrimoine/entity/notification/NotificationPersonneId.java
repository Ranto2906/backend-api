package com.seimad.patrimoine.entity.notification;

import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class NotificationPersonneId implements Serializable {

    private UUID idNotification;
    private Integer idPersonne;
}
