package com.seimad.patrimoine.repository.dossier;

import com.seimad.patrimoine.entity.dossier.SuiviDossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuiviDossierRepository extends JpaRepository<SuiviDossier, Integer> {

    List<SuiviDossier> findByDossierIdDossierOrderByEtapeOrdreAsc(Integer idDossier);

    Optional<SuiviDossier> findByDossierIdDossierAndEtapeIdEtape(Integer idDossier, Integer idEtape);

    boolean existsByDossierIdDossierAndEtapeIdEtape(Integer idDossier, Integer idEtape);

    boolean existsByEtapeIdEtape(Integer idEtape);
}
