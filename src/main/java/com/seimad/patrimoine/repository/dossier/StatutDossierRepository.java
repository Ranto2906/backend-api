package com.seimad.patrimoine.repository.dossier;

import com.seimad.patrimoine.entity.dossier.StatutDossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatutDossierRepository extends JpaRepository<StatutDossier, Integer> {

    Optional<StatutDossier> findByCode(String code);

    List<StatutDossier> findAllByOrderByOrdreAsc();
}
