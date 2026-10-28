package com.seimad.patrimoine.repository.dossier;

import com.seimad.patrimoine.entity.dossier.DossierDemandeur;
import com.seimad.patrimoine.entity.dossier.DossierDemandeurId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DossierDemandeurRepository extends JpaRepository<DossierDemandeur, DossierDemandeurId> {

    List<DossierDemandeur> findByIdDossier(Integer idDossier);

    /** Demandeurs de plusieurs dossiers en une seule requête (occupation propriété). */
    List<DossierDemandeur> findByIdDossierIn(Collection<Integer> idDossiers);

    Optional<DossierDemandeur> findByIdDossierAndIdPersonne(Integer idDossier, Integer idPersonne);

    boolean existsByIdDossierAndIdPersonne(Integer idDossier, Integer idPersonne);
}
