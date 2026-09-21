package com.seimad.patrimoine.repository.dossier;

import com.seimad.patrimoine.entity.dossier.TypeDossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TypeDossierRepository extends JpaRepository<TypeDossier, Integer> {

    Optional<TypeDossier> findByLibelleIgnoreCase(String libelle);

    Optional<TypeDossier> findByCodeIgnoreCase(String code);
}