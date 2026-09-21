package com.seimad.patrimoine.repository.dossier;

import com.seimad.patrimoine.entity.dossier.DossierFournie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DossierFournieRepository extends JpaRepository<DossierFournie, Integer> {

    Optional<DossierFournie> findByLibelleIgnoreCase(String libelle);
}