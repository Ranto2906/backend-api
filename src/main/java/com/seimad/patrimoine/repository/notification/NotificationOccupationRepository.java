package com.seimad.patrimoine.repository.notification;

import com.seimad.patrimoine.entity.notification.NotificationOccupation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationOccupationRepository extends JpaRepository<NotificationOccupation, UUID> {

    List<NotificationOccupation> findByStatut(String statut);

    List<NotificationOccupation> findByAnnee(Integer annee);

    List<NotificationOccupation> findByTitreFoncierIdTitreFoncier(UUID idTitreFoncier);

    List<NotificationOccupation> findByParcelleIdParcelle(UUID idParcelle);

    @Query("SELECT n FROM NotificationOccupation n WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(n.informationsOccupants) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(n.constats) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(n.doleances) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<NotificationOccupation> search(@Param("search") String search, Pageable pageable);

    /** Doublons d'import potentiels : même titre foncier, même parcelle, même date de notification. */
    @Query("SELECT n FROM NotificationOccupation n WHERE n.titreFoncier.idTitreFoncier = :tf " +
           "AND n.dateNotification = :date AND " +
           "((:parc IS NULL AND n.parcelle IS NULL) OR (:parc IS NOT NULL AND n.parcelle.idParcelle = :parc))")
    List<NotificationOccupation> trouverDoublons(@Param("tf") UUID idTitreFoncier,
                                                 @Param("parc") UUID idParcelle,
                                                 @Param("date") LocalDate dateNotification);

    long countByStatut(String statut);
    long countByAnnee(Integer annee);
}
