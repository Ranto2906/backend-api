package com.seimad.patrimoine.repository.notification;

import com.seimad.patrimoine.entity.notification.NotificationPersonne;
import com.seimad.patrimoine.entity.notification.NotificationPersonneId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationPersonneRepository extends JpaRepository<NotificationPersonne, NotificationPersonneId> {

    List<NotificationPersonne> findByIdNotification(UUID idNotification);

    List<NotificationPersonne> findByIdPersonne(Integer idPersonne);

    void deleteByIdNotification(UUID idNotification);
}
