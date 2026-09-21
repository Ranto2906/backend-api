package com.seimad.patrimoine.repository.referentiel;

import com.seimad.patrimoine.entity.referentiel.Propriete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProprieteRepository extends JpaRepository<Propriete, Integer> {

    List<Propriete> findByLieuVilleIdVille(Integer idVille);

    Optional<Propriete> findByNomIgnoreCase(String nom);

    Optional<Propriete> findFirstByNomIgnoreCase(String nom);

    Optional<Propriete> findByNumero(String numero);

    @Query("SELECT p FROM Propriete p WHERE LOWER(p.nom) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Propriete> search(@Param("search") String search);
}
