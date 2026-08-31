package com.seimad.patrimoine.repository.auth;

import com.seimad.patrimoine.entity.auth.JournalConnexion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalConnexionRepository extends JpaRepository<JournalConnexion, Integer> {

    List<JournalConnexion> findByUtilisateurIdUtilisateurOrderByDateConnexionDesc(Integer idUtilisateur);

    Page<JournalConnexion> findAllByOrderByDateConnexionDesc(Pageable pageable);

    @Query("SELECT j FROM JournalConnexion j WHERE j.utilisateur.idUtilisateur = :idUtilisateur " +
           "AND j.dateConnexion = (SELECT MAX(j2.dateConnexion) FROM JournalConnexion j2 " +
           "WHERE j2.utilisateur.idUtilisateur = :idUtilisateur AND j2.succes = true)")
    JournalConnexion findLastSuccessfulConnection(@Param("idUtilisateur") Integer idUtilisateur);

    long countByUtilisateurIdUtilisateurAndSuccesFalse(Integer idUtilisateur);
}
