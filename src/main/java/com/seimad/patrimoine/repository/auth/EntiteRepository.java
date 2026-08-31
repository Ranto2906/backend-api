package com.seimad.patrimoine.repository.auth;

import com.seimad.patrimoine.entity.auth.Entite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntiteRepository extends JpaRepository<Entite, Integer> {

    Optional<Entite> findByCodeEntite(String codeEntite);

    boolean existsByCodeEntite(String codeEntite);

    List<Entite> findByModuleIdModule(Integer idModule);

    @Query("SELECT e FROM Entite e JOIN FETCH e.module m WHERE m.idModule = :idModule OR :idModule IS NULL")
    List<Entite> findByModuleIdOptional(@Param("idModule") Integer idModule);
}
