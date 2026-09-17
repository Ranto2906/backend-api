package com.seimad.patrimoine.repository.dossier;

import com.seimad.patrimoine.entity.dossier.TypeDossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TypeDossierRepository extends JpaRepository<TypeDossier, Integer> {
}