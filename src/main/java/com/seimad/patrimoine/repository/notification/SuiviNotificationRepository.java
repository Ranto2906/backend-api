package com.seimad.patrimoine.repository.notification;

import com.seimad.patrimoine.entity.notification.SuiviNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SuiviNotificationRepository extends JpaRepository<SuiviNotification, UUID> {

    List<SuiviNotification> findByNotificationOccupationIdNotificationOrderByOrdreAsc(UUID idNotification);

    Optional<SuiviNotification> findByNotificationOccupationIdNotificationAndOrdre(UUID idNotification, Integer ordre);

    long countByNotificationOccupationIdNotification(UUID idNotification);

    @Query("SELECT s FROM SuiviNotification s WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(s.constats) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(s.actionsASuivre) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(s.notificationOccupation.titreFoncier.numero) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(s.notificationOccupation.parcelle.numeroLot) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<SuiviNotification> search(@Param("search") String search, Pageable pageable);
}
