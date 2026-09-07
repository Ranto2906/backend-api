package com.seimad.patrimoine.repository.signalement;

import com.seimad.patrimoine.entity.signalement.Assignation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignationRepository extends JpaRepository<Assignation, Long> {

    List<Assignation> findBySignalementIdSignalementOrderByDateAssignationDesc(UUID idSignalement);

    List<Assignation> findByNotificationIdNotification(UUID idNotification);

    List<Assignation> findByAvertissementIdAvertissement(UUID idAvertissement);

    Optional<Assignation> findBySignalementIdSignalementAndNotificationIdNotification(
            UUID idSignalement, UUID idNotification);

    Optional<Assignation> findBySignalementIdSignalementAndAvertissementIdAvertissement(
            UUID idSignalement, UUID idAvertissement);

    boolean existsBySignalementIdSignalementAndNotificationIdNotification(UUID idSignalement, UUID idNotification);

    boolean existsBySignalementIdSignalementAndAvertissementIdAvertissement(UUID idSignalement, UUID idAvertissement);

    long countBySignalementIdSignalement(UUID idSignalement);

    void deleteBySignalementIdSignalement(UUID idSignalement);
}
