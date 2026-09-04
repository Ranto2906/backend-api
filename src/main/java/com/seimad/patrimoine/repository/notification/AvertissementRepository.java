package com.seimad.patrimoine.repository.notification;

import com.seimad.patrimoine.entity.notification.Avertissement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AvertissementRepository extends JpaRepository<Avertissement, UUID> {

    List<Avertissement> findByPersonneIdPersonne(Integer idPersonne);

    List<Avertissement> findByAnnee(Integer annee);

    List<Avertissement> findByTitreFoncierIdTitreFoncier(UUID idTitreFoncier);

    List<Avertissement> findByParcelleIdParcelle(UUID idParcelle);

    @Query("SELECT a FROM Avertissement a WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(a.informationsOccupants) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(a.constats) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(a.mission) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<Avertissement> search(@Param("search") String search, Pageable pageable);

    /** Doublons d'import potentiels : même titre, même parcelle, même date et même personne. */
    @Query("SELECT a FROM Avertissement a WHERE a.titreFoncier.idTitreFoncier = :tf " +
           "AND a.dateAvertissement = :date AND " +
           "((:parc IS NULL AND a.parcelle IS NULL) OR (:parc IS NOT NULL AND a.parcelle.idParcelle = :parc)) AND " +
           "((:pers IS NULL AND a.personne IS NULL) OR (:pers IS NOT NULL AND a.personne.idPersonne = :pers))")
    List<Avertissement> trouverDoublons(@Param("tf") UUID idTitreFoncier,
                                        @Param("parc") UUID idParcelle,
                                        @Param("date") java.time.LocalDate dateAvertissement,
                                        @Param("pers") Integer idPersonne);

    long countByAnnee(Integer annee);
}
