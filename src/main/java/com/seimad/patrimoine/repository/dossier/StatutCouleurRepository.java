package com.seimad.patrimoine.repository.dossier;

import com.seimad.patrimoine.entity.dossier.StatutCouleur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatutCouleurRepository extends JpaRepository<StatutCouleur, Integer> {

    Optional<StatutCouleur> findByCode(String code);
}
