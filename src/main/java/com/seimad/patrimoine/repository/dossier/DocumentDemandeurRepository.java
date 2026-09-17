package com.seimad.patrimoine.repository.dossier;

import com.seimad.patrimoine.entity.dossier.DocumentDemandeur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentDemandeurRepository extends JpaRepository<DocumentDemandeur, Integer> {

    List<DocumentDemandeur> findByPersonne_IdPersonneOrderByDateUploadAsc(Integer idPersonne);
}
