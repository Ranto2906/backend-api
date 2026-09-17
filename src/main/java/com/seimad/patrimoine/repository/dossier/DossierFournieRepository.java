package com.seimad.patrimoine.repository.dossier;

import com.seimad.patrimoine.entity.dossier.DossierFournie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DossierFournieRepository extends JpaRepository<DossierFournie, Integer> {
}