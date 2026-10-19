package com.seimad.patrimoine.repository.notification;

import com.seimad.patrimoine.entity.notification.EtapeNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EtapeNotificationRepository extends JpaRepository<EtapeNotification, Integer> {

    List<EtapeNotification> findAllByOrderByOrdreAsc();

    Optional<EtapeNotification> findByCodeEtape(String codeEtape);

    Optional<EtapeNotification> findByOrdre(Integer ordre);
}
