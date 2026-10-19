package com.seimad.patrimoine.repository.referentiel;

import com.seimad.patrimoine.entity.referentiel.TypePropriete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TypeProprieteRepository extends JpaRepository<TypePropriete, Integer> {

    List<TypePropriete> findAllByOrderByLibelleAsc();

    Optional<TypePropriete> findByLibelleIgnoreCase(String libelle);

    Optional<TypePropriete> findFirstByLibelleIgnoreCase(String libelle);

    @Query("SELECT count(p) FROM Propriete p WHERE p.typePropriete.idTypePropriete = :idType")
    long countByTypeUtilise(@Param("idType") Integer idTypePropriete);
}
