package com.seimad.patrimoine.repository.notification;

import com.seimad.patrimoine.entity.notification.SuiviNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SuiviNotificationRepository extends JpaRepository<SuiviNotification, UUID> {

    List<SuiviNotification> findByNotificationOccupationIdNotificationOrderByOrdreAsc(UUID idNotification);

    Optional<SuiviNotification> findByNotificationOccupationIdNotificationAndOrdre(UUID idNotification, Integer ordre);

    long countByNotificationOccupationIdNotification(UUID idNotification);
}
