package com.seimad.patrimoine.repository.dossier;

import com.seimad.patrimoine.entity.dossier.Audit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<Audit, Integer> {

    Page<Audit> findAllByOrderByDateActionDesc(Pageable pageable);

    List<Audit> findByUtilisateurIdUtilisateurOrderByDateActionDesc(Integer idUtilisateur);

    List<Audit> findByEntiteTypeOrderByDateActionDesc(String entiteType);

    @Query("SELECT a FROM Audit a WHERE " +
           "(:entiteType IS NULL OR :entiteType = '' OR a.entiteType = :entiteType) " +
           "AND (:action IS NULL OR :action = '' OR a.action = :action) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "LOWER(a.entiteId) LIKE LOWER(CONCAT('%',:search,'%'))) " +
           "ORDER BY a.dateAction DESC")
    Page<Audit> search(@Param("entiteType") String entiteType,
                       @Param("action") String action,
                       @Param("search") String search,
                       Pageable pageable);

    long countByEntiteType(String entiteType);

    long countByAction(String action);
}
