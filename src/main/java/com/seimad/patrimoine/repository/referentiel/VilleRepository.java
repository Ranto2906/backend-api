package com.seimad.patrimoine.repository.referentiel;

import com.seimad.patrimoine.entity.referentiel.Ville;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VilleRepository extends JpaRepository<Ville, Integer> {

    Optional<Ville> findByNomVille(String nomVille);

    boolean existsByNomVille(String nomVille);

    @Query("SELECT v FROM Ville v WHERE LOWER(v.nomVille) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Ville> search(@Param("search") String search);
}
